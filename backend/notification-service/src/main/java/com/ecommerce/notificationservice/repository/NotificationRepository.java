package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByOrderId(String orderId);
}
