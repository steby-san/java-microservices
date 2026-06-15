package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import com.ecommerce.notificationservice.event.OrderItemEvent;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Property-based tests for MockEmailService.
 * Feature: notification-service
 */
class EmailServicePropertyTest {

    private final MockEmailService emailService = new MockEmailService();

    // Feature: notification-service, Property 1: Email subject format
    // Validates: Yêu cầu 2.1
    @Property(tries = 100)
    void emailSubjectFormatIsCorrect(@ForAll String orderId) {
        String subject = emailService.buildEmailSubject(orderId);
        assert subject.equals("Xác nhận đơn hàng #" + orderId)
                : "Expected subject 'Xác nhận đơn hàng #" + orderId + "' but got '" + subject + "'";
    }

    // Feature: notification-service, Property 2: Email content completeness
    // Validates: Yêu cầu 2.2
    @Property(tries = 100)
    void emailContentIsComplete(
            @ForAll String orderId,
            @ForAll @net.jqwik.api.constraints.BigRange(min = "0", max = "9999") BigDecimal totalAmount,
            @ForAll("orderItems") List<OrderItemEvent> items
    ) {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                "user-1",
                "user@example.com",
                totalAmount,
                items,
                createdAt
        );

        String content = emailService.buildEmailContent(event);

        // Assert: content contains orderId
        assert content.contains(orderId)
                : "Content should contain orderId: " + orderId;

        // Assert: content contains totalAmount formatted to 2 decimal places
        String formattedAmount = String.format("%.2f", totalAmount);
        assert content.contains(formattedAmount)
                : "Content should contain totalAmount formatted as '" + formattedAmount + "'";

        // Assert: content contains each item's productName and quantity
        for (OrderItemEvent item : items) {
            assert content.contains(item.productName())
                    : "Content should contain item productName: " + item.productName();
            assert content.contains(String.valueOf(item.quantity()))
                    : "Content should contain item quantity: " + item.quantity();
        }

        // Assert: content contains createdAt in ISO 8601 format
        assert content.contains(createdAt.toString())
                : "Content should contain createdAt ISO 8601: " + createdAt;
    }

    @Provide
    Arbitrary<List<OrderItemEvent>> orderItems() {
        Arbitrary<String> productNames = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(20);
        Arbitrary<Integer> quantities = Arbitraries.integers().between(1, 100);

        Arbitrary<OrderItemEvent> itemArbitrary = Combinators.combine(productNames, quantities)
                .as(OrderItemEvent::new);

        return itemArbitrary.list().ofMinSize(1).ofMaxSize(5);
    }
}
