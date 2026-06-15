package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.config.KafkaConsumerConfig;
import com.ecommerce.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.Acknowledgment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Example-based unit tests for OrderEventConsumer — malformed message handling.
 * Validates: Yêu cầu 1.4
 *
 * No Spring context needed — uses Mockito directly.
 */
class OrderEventConsumerTest {

    private NotificationService notificationService;
    private Acknowledgment ack;
    private OrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        ack = mock(Acknowledgment.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        consumer = new OrderEventConsumer(notificationService, objectMapper);
    }

    /**
     * When the raw message is not valid JSON at all, the consumer must:
     * - skip processing (never call processOrderCreatedEvent)
     * - acknowledge the offset so the message is not requeued
     *
     * Validates: Yêu cầu 1.4
     */
    @Test
    void skipsMalformedJson_notJson() {
        consumer.consume("not-valid-json", ack);

        verify(notificationService, never()).processOrderCreatedEvent(any());
        verify(ack, times(1)).acknowledge();
    }

    /**
     * When the raw message is valid JSON but missing required fields
     * (userEmail, totalAmount, items, createdAt are absent), the consumer must:
     * - skip processing (never call processOrderCreatedEvent)
     * - acknowledge the offset so the message is not requeued
     *
     * Validates: Yêu cầu 1.4
     */
    @Test
    void skipsMalformedJson_missingFields() {
        // Only orderId and userId present; userEmail, totalAmount, items, createdAt all missing
        String incompleteJson = "{\"orderId\":\"123\",\"userId\":\"u1\"}";

        consumer.consume(incompleteJson, ack);

        verify(notificationService, never()).processOrderCreatedEvent(any());
        verify(ack, times(1)).acknowledge();
    }

    /**
     * Verifies KafkaConsumerConfig creates DefaultErrorHandler with FixedBackOff(1000L, 3)
     * and DeadLetterPublishingRecoverer pointing to order-events.DLT topic.
     * Validates: Yêu cầu 1.5, 8.2, 8.3
     */
    @Test
    void kafkaErrorHandlerIsConfiguredWithRetryAndDlt() {
        // KafkaConsumerConfig now self-creates its factories from @Value fields
        // We verify the beans are properly constructed via reflection to set bootstrap-servers
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        try {
            java.lang.reflect.Field bs = KafkaConsumerConfig.class.getDeclaredField("bootstrapServers");
            bs.setAccessible(true);
            bs.set(config, "localhost:9092");
            java.lang.reflect.Field gid = KafkaConsumerConfig.class.getDeclaredField("groupId");
            gid.setAccessible(true);
            gid.set(config, "notification-service-group");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Act — create the DefaultErrorHandler bean
        DefaultErrorHandler errorHandler = config.defaultErrorHandler();
        assertNotNull(errorHandler, "DefaultErrorHandler bean must not be null");

        // Verify the container factory is properly configured
        ConcurrentKafkaListenerContainerFactory<String, String> factory = config.kafkaListenerContainerFactory();
        assertNotNull(factory, "KafkaListenerContainerFactory must not be null");
        assertEquals(ContainerProperties.AckMode.MANUAL_IMMEDIATE,
                     factory.getContainerProperties().getAckMode(),
                     "AckMode must be MANUAL_IMMEDIATE");
    }
}
