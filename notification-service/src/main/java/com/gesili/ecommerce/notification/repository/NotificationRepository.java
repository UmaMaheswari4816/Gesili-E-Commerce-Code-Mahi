package com.gesili.ecommerce.notification.repository;

import com.gesili.ecommerce.notification.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByOrderId(UUID orderId);
}
