package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import com.ecommerce.notificationservice.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order-events topic.
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 8.1, 8.2, 8.3, 8.4
 */
@Component
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "order-events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String rawMessage, Acknowledgment ack) {
        // Step 1 — Deserialize
        OrderCreatedEvent event;
        try {
            event = objectMapper.readValue(rawMessage, OrderCreatedEvent.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize message, skipping. rawMessage={}", rawMessage, e);
            ack.acknowledge();
            return;
        }

        // Step 2 — Validate: all 6 fields must be non-null
        if (event.orderId() == null
                || event.userId() == null
                || event.userEmail() == null
                || event.totalAmount() == null
                || event.items() == null
                || event.createdAt() == null) {
            logger.error("Message has missing required fields, skipping. rawMessage={}", rawMessage);
            ack.acknowledge();
            return;
        }

        // Step 3 — Process: exceptions propagate to DefaultErrorHandler for retry/DLT
        notificationService.processOrderCreatedEvent(event);
        ack.acknowledge();
    }
}
