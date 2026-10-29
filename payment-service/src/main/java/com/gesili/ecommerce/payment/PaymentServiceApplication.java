package com.gesili.ecommerce.payment;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment service.
 *
 * <p>On {@code InventoryReserved} it creates a provider order/intent (Razorpay by default,
 * Stripe via the provider Strategy) and emits checkout params. The customer completes
 * checkout at the provider, which calls back to a public, signature-verified, idempotent
 * webhook that maps to {@code PaymentCompleted}/{@code PaymentFailed}. A late payment on an
 * already-cancelled/timed-out order is auto-refunded ({@code PaymentRefunded}). A
 * reconciliation job (ShedLock) is the safety net for missed webhooks.
 */
@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
