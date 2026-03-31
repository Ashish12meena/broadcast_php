package com.aigreentick.services.broadcast.client.service;

import com.aigreentick.services.broadcast.client.dto.MessageResultCallbackRequest;

/**
 * Client for reporting message send results back to the WhatsappMessage service.
 * Called asynchronously after each window completes — fire-and-forget.
 */
public interface MessagingCallbackClient {

    /**
     * Report the results of a window of Meta API calls back to the messaging service.
     * Fire-and-forget — broadcast service does not wait for the response.
     */
    void reportResults(MessageResultCallbackRequest request);
}
