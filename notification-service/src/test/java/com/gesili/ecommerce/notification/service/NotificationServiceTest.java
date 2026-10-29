package com.gesili.ecommerce.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gesili.ecommerce.common.event.EventTypes;
import com.gesili.ecommerce.notification.channel.NotificationChannel;
import com.gesili.ecommerce.notification.entity.Notification;
import com.gesili.ecommerce.notification.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for templating, channel dispatch, and logging — no Kafka/DB. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository repo;

    private NotificationChannel recordingEmail(StringBuilder sink) {
        return new NotificationChannel() {
            @Override
            public String name() {
                return "email";
            }

            @Override
            public boolean send(String recipient, String subject, String body) {
                sink.append(subject).append("|").append(body);
                return true;
            }
        };
    }

    @Test
    void orderConfirmed_rendersAndLogs() {
        StringBuilder sent = new StringBuilder();
        var service = new NotificationService(List.of(recordingEmail(sent)), repo);
        when(repo.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        UUID orderId = UUID.randomUUID();

        service.notify(EventTypes.ORDER_CONFIRMED, orderId, "jane@example.com", "email", null);

        assertThat(sent.toString()).contains("confirmed");
        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getEventType()).isEqualTo(EventTypes.ORDER_CONFIRMED);
        assertThat(cap.getValue().isDelivered()).isTrue();
    }

    @Test
    void orderCancelled_includesReason() {
        StringBuilder sent = new StringBuilder();
        var service = new NotificationService(List.of(recordingEmail(sent)), repo);
        when(repo.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        service.notify(EventTypes.ORDER_CANCELLED, UUID.randomUUID(), "jane@example.com", "email", "PAYMENT_TIMEOUT");

        assertThat(sent.toString()).contains("cancelled").contains("PAYMENT_TIMEOUT");
    }

    @Test
    void unknownChannel_fallsBackToEmail() {
        StringBuilder sent = new StringBuilder();
        var service = new NotificationService(List.of(recordingEmail(sent)), repo);
        when(repo.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        service.notify(EventTypes.PAYMENT_COMPLETED, UUID.randomUUID(), "jane@example.com", "carrier-pigeon", null);

        assertThat(sent.toString()).isNotEmpty(); // used the email fallback
    }
}
