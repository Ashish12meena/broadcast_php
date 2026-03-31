package com.aigreentick.services.broadcast.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request body sent to WhatsappMessage service's callback endpoint
 * after processing a window of recipients (up to 80).
 *
 * POST /internal/broadcast/callbacks/message-results
 *
 * ADAPTED: Uses broadcastId + mobile instead of recipientId/messageId/contactId
 * to match WhatsappMessage service's report update needs.
 */
@Data
@Builder
public class MessageResultCallbackRequest {

    private Long campaignId;
    private String phoneNumberId;
    private List<RecipientResult> results;

    @Data
    @Builder
    public static class RecipientResult {

        /** Broadcast ID — used by messaging service to find the report row */
        private Long broadcastId;

        /** Phone number — used by messaging service to find the report row */
        private String mobile;

        /** true = Meta accepted the message */
        private boolean success;

        /** wamid from Meta response */
        private String providerMessageId;

        /** Meta's message_status: "accepted", "sent", etc. */
        private String messageStatus;

        /** JSON string of payload sent to Meta (for auditing) */
        private String payload;

        /** JSON string of Meta's raw response (for auditing) */
        private String response;

        /** Populated when success=false */
        private String errorCode;
        private String errorMessage;
    }
}
