package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.NotificationDTO;
import com.ecommerce.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for querying notifications.
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Returns all notifications, optionally filtered by orderId.
     * Requirements: 4.1, 4.2
     */
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getAll(
            @RequestParam(required = false) String orderId) {
        List<NotificationDTO> list = (orderId != null)
                ? notificationService.getNotificationsByOrderId(orderId)
                : notificationService.getAllNotifications();
        return ResponseEntity.ok(list);
    }

    /**
     * Returns a single notification by id.
     * ResourceNotFoundException is handled by GlobalExceptionHandler → 404.
     * Requirements: 4.3, 4.4, 4.5, 4.6
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationDTO> getById(@PathVariable Long id) {
        NotificationDTO dto = notificationService.getNotificationById(id);
        return ResponseEntity.ok(dto);
    }
}
