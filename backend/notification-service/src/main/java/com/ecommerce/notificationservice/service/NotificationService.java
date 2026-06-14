package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationDTO;
import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import com.ecommerce.notificationservice.exception.EmailSendException;
import com.ecommerce.notificationservice.exception.ResourceNotFoundException;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Business logic layer for notification processing and retrieval.
 * Requirements: 2.5, 2.6, 2.7, 3.1, 3.4, 4.1, 4.2, 4.3, 4.4, 4.6
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final EmailService emailService;
    private final NotificationRepository notificationRepository;

    public NotificationService(EmailService emailService, NotificationRepository notificationRepository) {
        this.emailService = emailService;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Processes an OrderCreatedEvent: validates email, sends confirmation, and persists result.
     * Requirements: 2.5, 2.6, 2.7, 3.1, 3.4
     */
    public void processOrderCreatedEvent(OrderCreatedEvent event) {
        String userEmail = event.userEmail();
        String orderId = event.orderId();

        // Build common notification fields
        String subject = emailService.buildEmailSubject(orderId);
        String content = emailService.buildEmailContent(event);

        // Requirement 2.7: invalid email → save INVALID_EMAIL, sentAt=null
        if (userEmail == null || !userEmail.contains("@")) {
            logger.warn("Invalid email address for orderId={}: '{}'", orderId, userEmail);
            Notification notification = buildNotification(event, subject, content, NotificationStatus.INVALID_EMAIL, null);
            saveNotification(notification, orderId);
            return;
        }

        // Requirement 2.5 / 2.6: attempt to send email
        NotificationStatus status;
        LocalDateTime sentAt;

        try {
            emailService.sendOrderConfirmation(event);
            // Requirement 2.5: success → SENT, sentAt = now UTC
            status = NotificationStatus.SENT;
            sentAt = LocalDateTime.now(ZoneOffset.UTC);
        } catch (EmailSendException e) {
            // Requirement 2.6: email failure → FAILED, sentAt=null
            logger.error("Failed to send email for orderId={}: {}", orderId, e.getMessage(), e);
            status = NotificationStatus.FAILED;
            sentAt = null;
        }

        Notification notification = buildNotification(event, subject, content, status, sentAt);
        saveNotification(notification, orderId);
    }

    /**
     * Returns all notifications mapped to DTOs.
     * Requirement: 4.1
     */
    public List<NotificationDTO> getAllNotifications() {
        return notificationRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Returns notifications for the given orderId mapped to DTOs.
     * Requirement: 4.2
     */
    public List<NotificationDTO> getNotificationsByOrderId(String orderId) {
        return notificationRepository.findByOrderId(orderId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Returns a single notification by id, or throws ResourceNotFoundException.
     * Requirements: 4.3, 4.4
     */
    public NotificationDTO getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
    }

    // --- Private helpers ---

    private Notification buildNotification(OrderCreatedEvent event,
                                           String subject,
                                           String content,
                                           NotificationStatus status,
                                           LocalDateTime sentAt) {
        Notification notification = new Notification();
        notification.setOrderId(event.orderId());
        notification.setUserId(event.userId());
        notification.setRecipientEmail(event.userEmail());
        notification.setSubject(subject);
        notification.setContent(content);
        notification.setStatus(status);
        notification.setSentAt(sentAt);
        // createdAt will be auto-set by @PrePersist
        return notification;
    }

    /**
     * Persists a notification; catches any DB exception to prevent propagation.
     * Requirement: 3.4
     */
    private void saveNotification(Notification notification, String orderId) {
        try {
            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Failed to save notification for orderId={}: {}", orderId, e.getMessage(), e);
        }
    }

    /**
     * Maps a Notification entity to a NotificationDTO.
     * Requirement: 4.6
     */
    private NotificationDTO toDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getOrderId(),
                notification.getUserId(),
                notification.getRecipientEmail(),
                notification.getSubject(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }
}
