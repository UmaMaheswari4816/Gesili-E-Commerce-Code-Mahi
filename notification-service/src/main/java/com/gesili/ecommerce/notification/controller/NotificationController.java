package com.gesili.ecommerce.notification.controller;

import com.gesili.ecommerce.common.constant.ApiConstants;
import com.gesili.ecommerce.notification.entity.Notification;
import com.gesili.ecommerce.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only view of the notification log (useful for debugging / admin). */
@RestController
@RequestMapping(ApiConstants.API_V1 + "/notifications")
@Tag(name = "Notifications", description = "Notification log")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Operation(summary = "List notifications sent for an order")
    @GetMapping("/order/{orderId}")
    public List<Notification> forOrder(@PathVariable UUID orderId) {
        return notifications.forOrder(orderId);
    }
}
