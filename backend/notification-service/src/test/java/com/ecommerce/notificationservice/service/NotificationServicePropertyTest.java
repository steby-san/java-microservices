package com.ecommerce.notificationservice.service;

// Feature: notification-service
// Property 3: Notification Status Mapping — Validates: Requirements 2.5, 2.6, 2.7, 3.1
// Property 7: CreatedAt UTC Auto-Set    — Validates: Requirements 3.2, 3.3

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import com.ecommerce.notificationservice.event.OrderItemEvent;
import com.ecommerce.notificationservice.exception.EmailSendException;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.mail.MailSendException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property 3: Notification Status Mapping
 * Validates: Requirements 2.5, 2.6, 2.7, 3.1
 *
 * For every OrderCreatedEvent, NotificationService must persist exactly one
 * Notification whose status reflects the processing outcome:
 *   (a) valid email + send success   → SENT,          sentAt != null
 *   (b) valid email + send exception → FAILED,        sentAt == null
 *   (c) invalid email (no '@')       → INVALID_EMAIL, sentAt == null
 *
 * Property 7: CreatedAt UTC Auto-Set
 * Validates: Requirements 3.2, 3.3
 *
 * When a Notification entity is persisted without setting createdAt,
 * the @PrePersist callback must auto-populate createdAt with the current UTC time.
 *
 * This test directly invokes the @PrePersist method to validate the logic
 * without requiring a full Spring context or database.
 */
class NotificationServicePropertyTest {

    // -----------------------------------------------------------------------
    // Property 3 — Notification Status Mapping
    // -----------------------------------------------------------------------

    /** Builds a minimal valid OrderCreatedEvent for the given email address. */
    private static OrderCreatedEvent makeEvent(String email) {
        return new OrderCreatedEvent(
                "order-001",
                "user-001",
                email,
                new BigDecimal("99.99"),
                List.of(new OrderItemEvent("Widget", 2)),
                LocalDateTime.of(2024, 1, 1, 12, 0)
        );
    }

    /**
     * Case (a): valid email + sendOrderConfirmation succeeds
     * → repository.save() called once with status=SENT and sentAt != null
     *
     * **Validates: Requirements 2.5, 3.1**
     */
    @Test
    void property3a_validEmail_sendSuccess_statusIsSentAndSentAtNotNull() throws Exception {
        // Arrange
        EmailService emailService = mock(EmailService.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);

        when(emailService.buildEmailSubject(any())).thenReturn("subject");
        when(emailService.buildEmailContent(any())).thenReturn("content");
        doNothing().when(emailService).sendOrderConfirmation(any());

        NotificationService service = new NotificationService(emailService, notificationRepository);

        OrderCreatedEvent event = makeEvent("user@example.com");

        // Act
        service.processOrderCreatedEvent(event);

        // Assert — save called exactly once
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(NotificationStatus.SENT, saved.getStatus(),
                "Status must be SENT when email send succeeds");
        assertNotNull(saved.getSentAt(),
                "sentAt must be non-null when email send succeeds");
    }

    /**
     * Case (b): valid email + sendOrderConfirmation throws EmailSendException
     * → repository.save() called once with status=FAILED and sentAt == null
     *
     * **Validates: Requirements 2.6, 3.1**
     */
    @Test
    void property3b_validEmail_sendFails_statusIsFailedAndSentAtNull() throws Exception {
        // Arrange
        EmailService emailService = mock(EmailService.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);

        when(emailService.buildEmailSubject(any())).thenReturn("subject");
        when(emailService.buildEmailContent(any())).thenReturn("content");

        MailSendException mailCause = new MailSendException("smtp error");
        doThrow(new EmailSendException("fail", mailCause))
                .when(emailService).sendOrderConfirmation(any());

        NotificationService service = new NotificationService(emailService, notificationRepository);

        OrderCreatedEvent event = makeEvent("user@example.com");

        // Act
        service.processOrderCreatedEvent(event);

        // Assert — save called exactly once
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(NotificationStatus.FAILED, saved.getStatus(),
                "Status must be FAILED when EmailSendException is thrown");
        assertNull(saved.getSentAt(),
                "sentAt must be null when email send fails");
    }

    /**
     * Case (c): email does not contain '@'
     * → repository.save() called once with status=INVALID_EMAIL and sentAt == null
     * → sendOrderConfirmation must NOT be called
     *
     * **Validates: Requirements 2.7, 3.1**
     */
    @Test
    void property3c_invalidEmail_statusIsInvalidEmailAndSentAtNull() throws Exception {
        // Arrange
        EmailService emailService = mock(EmailService.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);

        when(emailService.buildEmailSubject(any())).thenReturn("subject");
        when(emailService.buildEmailContent(any())).thenReturn("content");

        NotificationService service = new NotificationService(emailService, notificationRepository);

        // Email without '@'
        OrderCreatedEvent event = makeEvent("not-a-valid-email");

        // Act
        service.processOrderCreatedEvent(event);

        // Assert — save called exactly once
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(NotificationStatus.INVALID_EMAIL, saved.getStatus(),
                "Status must be INVALID_EMAIL when email has no '@'");
        assertNull(saved.getSentAt(),
                "sentAt must be null for INVALID_EMAIL");

        // sendOrderConfirmation must never be called for invalid emails
        verify(emailService, never()).sendOrderConfirmation(any());
    }

    // -----------------------------------------------------------------------
    // Property 7 — CreatedAt UTC Auto-Set
    // -----------------------------------------------------------------------

    /**
     * Property 7: For any valid Notification without createdAt set,
     * invoking the @PrePersist callback must auto-populate createdAt to a non-null value.
     *
     * Validates: Requirements 3.2, 3.3
     */
    @Property(tries = 100)
    void property7_createdAtIsAutoSetByPrePersist(
            @ForAll @NotBlank @StringLength(min = 1, max = 50) String orderId,
            @ForAll @NotBlank @StringLength(min = 1, max = 30) String userId,
            @ForAll @NotBlank @StringLength(min = 3, max = 100) String recipientEmail,
            @ForAll @NotBlank @StringLength(min = 1, max = 200) String subject,
            @ForAll @NotBlank @StringLength(min = 1, max = 1000) String content) {

        // Build entity WITHOUT setting createdAt
        Notification notification = new Notification();
        notification.setOrderId(orderId);
        notification.setUserId(userId);
        notification.setRecipientEmail(recipientEmail);
        notification.setSubject(subject);
        notification.setContent(content);
        notification.setStatus(NotificationStatus.SENT);
        // createdAt intentionally left null to test @PrePersist

        // Pre-condition: createdAt is null before @PrePersist fires
        assert notification.getCreatedAt() == null
                : "createdAt should be null before @PrePersist";

        // Act — invoke the @PrePersist callback directly
        notification.prePersist();

        // Assert — @PrePersist must have populated createdAt
        assertNotNull(notification.getCreatedAt(),
                "createdAt must be auto-set by @PrePersist when left null before persist");
    }
}
