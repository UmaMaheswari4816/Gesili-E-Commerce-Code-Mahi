package com.gecili.ecommerce.notification.service;

import com.gecili.ecommerce.notification.channel.NotificationChannel;
import com.gecili.ecommerce.notification.entity.Notification;
import com.gecili.ecommerce.notification.repository.NotificationRepository;
import com.gecili.ecommerce.notification.service.NotificationTemplates.Message;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders a template for an event, sends it over the configured channel(s), and logs the
 * result. Channel selection uses the Strategy beans; here we default to email (SMS available).
 */
@Slf4j
@Service
public class NotificationService {

    private final Map<String, NotificationChannel> channels;
    private final NotificationRepository log;

    public NotificationService(List<NotificationChannel> channelBeans, NotificationRepository log) {
        this.channels =
                channelBeans.stream().collect(Collectors.toMap(NotificationChannel::name, Function.identity()));
        this.log = log;
    }

    /** Render + send + log a notification for a saga event. */
    @Transactional
    public void notify(String eventType, UUID orderId, String recipient, String channelName, String reason) {
        Message msg = NotificationTemplates.render(eventType, orderId, reason);
        NotificationChannel channel = channels.getOrDefault(channelName, channels.get("email"));
        boolean delivered = channel.send(recipient, msg.subject(), msg.body());
        log.save(
                Notification.of(orderId, channel.name(), recipient, eventType, msg.subject(), msg.body(), delivered));
    }

    @Transactional(readOnly = true)
    public List<Notification> forOrder(UUID orderId) {
        return log.findByOrderId(orderId);
    }
}
