package com.ecommerce.notificationservice.consumer;

// Feature: notification-service, Property 4: Event deserialization round-trip
// Validates: Yêu cầu 1.2

import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import com.ecommerce.notificationservice.event.OrderItemEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property 4: Event Deserialization Round-Trip
 * Validates: Yêu cầu 1.2
 *
 * For any OrderCreatedEvent serialized to JSON and deserialized back,
 * all 6 fields must equal the original values.
 */
class OrderEventConsumerPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Fixed createdAt — jqwik doesn't auto-generate LocalDateTime
    private static final LocalDateTime FIXED_CREATED_AT = LocalDateTime.of(2024, 6, 15, 10, 0);

    // Feature: notification-service, Property 4: Event deserialization round-trip
    @Property(tries = 100)
    void eventDeserializationRoundTrip(
            @ForAll String orderId,
            @ForAll String userId,
            @ForAll String userEmail,
            @ForAll("totalAmounts") BigDecimal totalAmount,
            @ForAll("orderItems") List<OrderItemEvent> items
    ) throws Exception {
        OrderCreatedEvent original = new OrderCreatedEvent(
                orderId, userId, userEmail, totalAmount, items, FIXED_CREATED_AT);

        String json = objectMapper.writeValueAsString(original);
        OrderCreatedEvent deserialized = objectMapper.readValue(json, OrderCreatedEvent.class);

        assertEquals(orderId, deserialized.orderId(), "orderId must round-trip");
        assertEquals(userId, deserialized.userId(), "userId must round-trip");
        assertEquals(userEmail, deserialized.userEmail(), "userEmail must round-trip");
        assertEquals(0, totalAmount.compareTo(deserialized.totalAmount()),
                "totalAmount must round-trip (compareTo == 0)");
        assertEquals(items.size(), deserialized.items().size(), "items size must match");
        for (int i = 0; i < items.size(); i++) {
            assertEquals(items.get(i).productName(), deserialized.items().get(i).productName(),
                    "item[" + i + "].productName must round-trip");
            assertEquals(items.get(i).quantity(), deserialized.items().get(i).quantity(),
                    "item[" + i + "].quantity must round-trip");
        }
        assertEquals(FIXED_CREATED_AT, deserialized.createdAt(), "createdAt must round-trip");
    }

    @Provide
    Arbitrary<BigDecimal> totalAmounts() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("99999.99"))
                .ofScale(2);
    }

    @Provide
    Arbitrary<List<OrderItemEvent>> orderItems() {
        Arbitrary<String> names = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20);
        Arbitrary<Integer> quantities = Arbitraries.integers().between(1, 100);
        return Combinators.combine(names, quantities)
                .as(OrderItemEvent::new)
                .list()
                .ofMinSize(0)
                .ofMaxSize(5);
    }
}
