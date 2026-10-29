package com.gecili.ecommerce.notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SMS channel (dev stub). Logs the message instead of sending a real SMS. Replace {@link #send}
 * with a Twilio/SNS client for real delivery.
 */
@Slf4j
@Component
public class SmsChannel implements NotificationChannel {

    @Override
    public String name() {
        return "sms";
    }

    @Override
    public boolean send(String recipient, String subject, String body) {
        log.info("[SMS] to={} body='{}'", recipient, body);
        return true;
    }
}
