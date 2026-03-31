package com.aigreentick.services.broadcast.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Kafka event received from WhatsappMessage service on topic: whatsapp.messages.outbound
 *
 * Key:   wabaAccountId (= whatsappNoId from messaging service)
 * Value: JSON { campaignId, wabaAccountId, accessToken, payloads: [...] }
 *
 * wabaAccountId from messaging service = phoneNumberId in broadcast service context.
 * phoneNumberId drives everything:
 *   - Kafka partition key
 *   - Per-phone PhoneQueue key
 *   - Meta API path param: POST /{phoneNumberId}/messages
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BroadcastMessageEvent {

    private Long campaignId;

    /**
     * The sending phone number ID registered with Meta.
     * Messaging service sends this as "wabaAccountId".
     */
    @JsonProperty("wabaAccountId")
    private String phoneNumberId;

    /**
     * Bearer token for Meta API authentication.
     * Embedded by messaging service at dispatch time.
     */
    private String accessToken;

    /**
     * Recipients in this Kafka batch (up to 1000).
     */
    private List<RecipientPayload> payloads;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecipientPayload {

        /** Broadcast ID — passed through to callback for report update */
        private Long broadcastId;

        /** Phone number — passed through to callback for report update */
        private String mobile;

        /**
         * Complete Meta API request body (JSON string, snake_case).
         * Pre-built by WhatsappMessage service's WhatsappPayloadBuilder.
         * Ready to POST to /{phoneNumberId}/messages as-is.
         */
        private String requestPayload;
    }
}
