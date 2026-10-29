package com.gecili.ecommerce.order;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Order service: owns the order lifecycle and the choreography saga state.
 *
 * <p>Create-order writes the order and an outbox row in the SAME transaction (transactional
 * outbox); a single-writer relay then publishes outbox rows to Kafka. The service consumes
 * inventory + payment events to drive the order state machine, and runs a payment-timeout
 * sweep. ShedLock guarantees the relay and the timeout sweep run on one instance at a time.
 */
@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT2M")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
