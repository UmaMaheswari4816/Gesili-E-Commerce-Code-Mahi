package com.gesili.ecommerce.notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Email channel (dev stub). Logs the message instead of sending real mail, so the service
 * runs with no SMTP dependency. Replace the body of {@link #send} with a JavaMailSender call
 * for real delivery; nothing else changes.
 */
@Slf4j
@Component
public class EmailChannel implements NotificationChannel {

    @Override
    public String name() {
        return "email";
    }

    @Override
    public boolean send(String recipient, String subject, String body) {
        log.info("[EMAIL] to={} subject='{}' body='{}'", recipient, subject, body);
        return true;
    }
}
