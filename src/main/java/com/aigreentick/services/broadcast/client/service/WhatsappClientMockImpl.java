package com.aigreentick.services.broadcast.client.service;

import com.aigreentick.services.broadcast.client.dto.MetaApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock implementation for stress testing / local dev.
 * Simulates Meta WhatsApp Cloud API responses without making actual HTTP calls.
 */
@Slf4j
@Service
@Profile("stress-test | mock")
public class WhatsappClientMockImpl implements WhatsappClient {

    private static final Random random = new Random();
    private static final AtomicLong messageCounter = new AtomicLong(0);

    private static final int MIN_DELAY_MS = 50;
    private static final int MAX_DELAY_MS = 200;
    private static final double FAILURE_RATE = 0.05;
    private static final double ACCEPTANCE_RATE = 0.95;

    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong successCalls = new AtomicLong(0);
    private final AtomicLong failedCalls = new AtomicLong(0);

    @Override
    public MetaApiResponse sendMessage(String requestPayload, String phoneNumberId, String accessToken) {
        long callNumber = totalCalls.incrementAndGet();

        try {
            // Simulate network delay
            int delay = MIN_DELAY_MS + random.nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1);
            Thread.sleep(delay);

            if (random.nextDouble() < FAILURE_RATE) {
                failedCalls.incrementAndGet();
                return buildErrorResponse();
            }

            successCalls.incrementAndGet();
            return buildSuccessResponse(phoneNumberId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failedCalls.incrementAndGet();
            return buildErrorResponseWithMessage(500, "Request interrupted");
        }
    }

    private MetaApiResponse buildSuccessResponse(String phoneNumberId) {
        String wamid = "wamid." + UUID.randomUUID().toString().replace("-", "") + "_" + messageCounter.incrementAndGet();
        String status = random.nextDouble() < ACCEPTANCE_RATE ? "accepted" : "sent";

        MetaApiResponse.ContactDto contact = new MetaApiResponse.ContactDto();
        contact.setInput(phoneNumberId);
        contact.setWaId(phoneNumberId);

        MetaApiResponse.MessageDto message = new MetaApiResponse.MessageDto();
        message.setId(wamid);
        message.setMessageStatus(status);

        MetaApiResponse response = new MetaApiResponse();
        response.setMessagingProduct("whatsapp");
        response.setContacts(List.of(contact));
        response.setMessages(List.of(message));
        return response;
    }

    private MetaApiResponse buildErrorResponse() {
        String[] errorMessages = {"Rate limit exceeded", "Invalid phone number", "Template not found", "Network timeout"};
        int[] errorCodes = {429, 400, 404, 503};
        int idx = random.nextInt(errorMessages.length);
        return buildErrorResponseWithMessage(errorCodes[idx], errorMessages[idx]);
    }

    private MetaApiResponse buildErrorResponseWithMessage(int code, String message) {
        MetaApiResponse.ErrorDto error = new MetaApiResponse.ErrorDto();
        error.setCode(code);
        error.setMessage(message);
        error.setType("mock_error");

        MetaApiResponse response = new MetaApiResponse();
        response.setError(error);
        return response;
    }

    public void resetStatistics() {
        totalCalls.set(0);
        successCalls.set(0);
        failedCalls.set(0);
    }
}
