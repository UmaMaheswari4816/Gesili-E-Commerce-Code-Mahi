package com.gesili.ecommerce.notification.channel;

/**
 * Strategy for a delivery channel (email, SMS, ...). Adding a channel = one implementation;
 * the dispatcher picks channels without changing. Implementations are dev stubs that log the
 * message; swapping in a real SMTP/Twilio client is localized to {@link #send}.
 */
public interface NotificationChannel {

    /** Channel key, e.g. {@code "email"} / {@code "sms"}. */
    String name();

    /**
     * Deliver a message.
     *
     * @param recipient address/number
     * @param subject   subject (ignored by SMS)
     * @param body      rendered message body
     * @return true if accepted for delivery
     */
    boolean send(String recipient, String subject, String body);
}
