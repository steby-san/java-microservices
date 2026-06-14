package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import com.ecommerce.notificationservice.event.OrderItemEvent;
import com.ecommerce.notificationservice.exception.EmailSendException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Production email service implementation that sends real emails via JavaMailSender.
 * Active when notification.email.mock=false.
 * Requirements: 2.1, 2.2, 2.3
 */
@Service
@ConditionalOnProperty(name = "notification.email.mock", havingValue = "false")
public class RealEmailService implements EmailService {

    private final JavaMailSender mailSender;

    public RealEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an order confirmation email to the recipient specified in the event.
     *
     * @param event the order created event containing recipient and order details
     * @throws EmailSendException if the underlying mail provider fails (wraps MailException)
     */
    @Override
    public void sendOrderConfirmation(OrderCreatedEvent event) throws EmailSendException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.userEmail());
        message.setSubject(buildEmailSubject(event.orderId()));
        message.setText(buildEmailContent(event));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new EmailSendException(
                    "Failed to send order confirmation email for order: " + event.orderId(), ex);
        }
    }

    /**
     * Builds the subject line for the order confirmation email.
     *
     * @param orderId the order identifier
     * @return the email subject string
     */
    @Override
    public String buildEmailSubject(String orderId) {
        return "Xác nhận đơn hàng #" + orderId;
    }

    /**
     * Builds the text body for the order confirmation email.
     *
     * @param event the order created event
     * @return the email body content
     */
    @Override
    public String buildEmailContent(OrderCreatedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chào,\n\n");
        sb.append("Đơn hàng #").append(event.orderId()).append(" của bạn đã được xác nhận.\n\n");
        sb.append("Tổng tiền: ").append(String.format("%.2f", event.totalAmount())).append("\n\n");
        sb.append("Sản phẩm:\n");
        for (OrderItemEvent item : event.items()) {
            sb.append("- ").append(item.productName()).append(" x ").append(item.quantity()).append("\n");
        }
        sb.append("\nNgày đặt hàng: ").append(event.createdAt().toString()).append("\n\n");
        sb.append("Cảm ơn bạn đã mua hàng!");
        return sb.toString();
    }
}
