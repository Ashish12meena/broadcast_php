package com.aigreentick.services.broadcast.kafka.consumer;

import com.aigreentick.services.broadcast.kafka.event.BroadcastMessageEvent;
import com.aigreentick.services.broadcast.service.BatchCoordinatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BroadcastMessageConsumer {

    private final BatchCoordinatorService batchCoordinator;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.outbound-messages:whatsapp.broadcast.dispatch}",
            groupId = "${spring.kafka.consumer.group-id:broadcast-service}",
            containerFactory = "broadcastKafkaListenerFactory"
    )
    public void consume(
            @Payload String rawMessage,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String kafkaKey,
            Acknowledgment acknowledgment) {

        log.info("Received broadcast batch: kafkaKey={} partition={} offset={}",
                kafkaKey, partition, offset);

        try {
            BroadcastMessageEvent event = objectMapper.readValue(rawMessage, BroadcastMessageEvent.class);

            log.info("Parsed broadcast event: phoneNumberId={} recipients={}",
                    event.getPhoneNumberId(),
                    event.getPayloads() != null ? event.getPayloads().size() : 0);

            batchCoordinator.addBatch(event, acknowledgment);

        } catch (Exception e) {
            log.error("Failed to parse broadcast event: kafkaKey={} partition={} offset={} error={}",
                    kafkaKey, partition, offset, e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}