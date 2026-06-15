package com.ecommerce.notificationservice.dto;

import com.ecommerce.notificationservice.entity.NotificationStatus;
import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        String orderId,
        String userId,
        String recipientEmail,
        String subject,
        NotificationStatus status,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {
}
