package com.aigreentick.services.broadcast.config;

public final class ConfigConstants {

    private ConfigConstants() {}

    /**
     * Max concurrent WhatsApp API requests per phone number.
     * Meta allows up to 80 concurrent requests per WABA phone number.
     */
    public static final int MAX_CONCURRENT_WHATSAPP_REQUESTS = 50;
}
