package com.gesili.ecommerce.order.config;

import com.gesili.ecommerce.common.event.EventTypes;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Kafka wiring: topic creation (3 partitions each), and a consumer error handler with
 * exponential-backoff retries that routes exhausted/non-retryable records to
 * {@code <topic>.DLT}.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic orderEvents() {
        return TopicBuilder.name(EventTypes.TOPIC_ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEvents() {
        return TopicBuilder.name(EventTypes.TOPIC_INVENTORY_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentEvents() {
        return TopicBuilder.name(EventTypes.TOPIC_PAYMENT_EVENTS).partitions(3).replicas(1).build();
    }

    /**
     * Retry transient failures with exponential backoff, then publish to {@code <topic>.DLT}.
     * The DeadLetterPublishingRecoverer sends to a topic named by the original topic + ".DLT".
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        template,
                        (record, ex) ->
                                new org.apache.kafka.common.TopicPartition(
                                        record.topic() + EventTypes.DLT_SUFFIX, record.partition()));
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(15000L); // ~1s,2s,4s,8s then DLT
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
