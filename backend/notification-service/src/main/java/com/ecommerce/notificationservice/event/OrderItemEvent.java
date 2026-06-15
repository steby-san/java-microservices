package com.ecommerce.notificationservice.event;

/**
 * Represents an individual item within an order event.
 * Requirement: 1.2
 */
public record OrderItemEvent(
        String productName,
        int quantity
) {
}
