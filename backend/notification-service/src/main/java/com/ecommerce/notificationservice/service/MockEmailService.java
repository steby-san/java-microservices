package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import com.ecommerce.notificationservice.event.OrderItemEvent;
import com.ecommerce.notificationservice.exception.EmailSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mock implementation of EmailService that logs email content instead of sending real emails.
 * Active by default (matchIfMissing = true) — safe for dev/test environments.
 * Requirements: 2.1, 2.2, 2.4
 */
@Service
@ConditionalOnProperty(name = "notification.email.mock", havingValue = "true", matchIfMissing = true)
public class MockEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(MockEmailService.class);

    /**
     * Builds the email subject for the given order ID.
     *
     * @param orderId the order identifier
     * @return subject string in Vietnamese confirmation format
     */
    @Override
    public String buildEmailSubject(String orderId) {
        return "Xác nhận đơn hàng #" + orderId;
    }

    /**
     * Builds the plain-text email body containing order details.
     * totalAmount is formatted to 2 decimal places.
     * Each item is formatted as "productName x quantity" on its own line.
     * createdAt is formatted as ISO 8601 via LocalDateTime.toString().
     *
     * @param event the order created event
     * @return the formatted email body
     */
    @Override
    public String buildEmailContent(OrderCreatedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mã đơn hàng: ").append(event.orderId()).append("\n");
        sb.append("Tổng tiền: ").append(String.format("%.2f", event.totalAmount())).append("\n");
        sb.append("Sản phẩm:\n");
        for (OrderItemEvent item : event.items()) {
            sb.append(item.productName()).append(" x ").append(item.quantity()).append("\n");
        }
        sb.append("Thời gian tạo: ").append(event.createdAt().toString());
        return sb.toString();
    }

    /**
     * Logs the full email content (recipient, subject, body) at INFO level.
     * Never throws an exception.
     *
     * @param event the order created event
     */
    @Override
    public void sendOrderConfirmation(OrderCreatedEvent event) throws EmailSendException {
        String subject = buildEmailSubject(event.orderId());
        String body = buildEmailContent(event);
        log.info("=== [MockEmailService] Simulated email ===\nTo: {}\nSubject: {}\nBody:\n{}",
                event.userEmail(), subject, body);
    }
}
