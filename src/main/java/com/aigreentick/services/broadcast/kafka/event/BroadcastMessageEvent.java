package com.aigreentick.services.broadcast.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Kafka event consumed from topic: whatsapp.broadcast.dispatch
 *
 * Key:   wabaAccountId (= phoneNumberId)
 * Value: JSON { wabaAccountId, accessToken, payloads: [...] }
 *
 * No campaignId — the queue is keyed by phoneNumberId only.
 * Each recipient carries its own broadcastId, which is the sole
 * identifier propagated through to the callback for report updates.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BroadcastMessageEvent {

    /**
     * The sending phone number ID registered with Meta.
     * Messaging service sends this as "wabaAccountId".
     * Drives everything:
     *   - Kafka partition key
     *   - Per-phone PhoneQueue key
     *   - Meta API path param: POST /{phoneNumberId}/messages
     */
    @JsonProperty("wabaAccountId")
    private String phoneNumberId;

    /**
     * Bearer token for Meta API authentication.
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
         * Complete Meta API request body (JSON string).
         * Pre-built by WhatsappMessage service's WhatsappPayloadBuilder.
         */
        private String requestPayload;
    }
}