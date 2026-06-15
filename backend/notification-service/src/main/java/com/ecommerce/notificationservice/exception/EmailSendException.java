package com.ecommerce.notificationservice.exception;

import org.springframework.mail.MailException;

/**
 * Checked exception wrapping Spring's MailException for email sending failures.
 * Requirements: 2.3, 2.4
 */
public class EmailSendException extends Exception {

    public EmailSendException(String message, MailException cause) {
        super(message, cause);
    }
}
