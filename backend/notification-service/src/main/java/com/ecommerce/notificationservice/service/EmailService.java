package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import com.ecommerce.notificationservice.exception.EmailSendException;

/**
 * Service interface for sending order confirmation emails.
 * Requirements: 2.1, 2.2, 2.3, 2.4
 */
public interface EmailService {

    /**
     * Sends an order confirmation email for the given event.
     *
     * @param event the order created event containing recipient and order details
     * @throws EmailSendException if the underlying mail provider fails
     */
    void sendOrderConfirmation(OrderCreatedEvent event) throws EmailSendException;

    /**
     * Builds the HTML/text body for the order confirmation email.
     *
     * @param event the order created event
     * @return the email body content
     */
    String buildEmailContent(OrderCreatedEvent event);

    /**
     * Builds the subject line for the order confirmation email.
     *
     * @param orderId the order identifier
     * @return the email subject string
     */
    String buildEmailSubject(String orderId);
}
