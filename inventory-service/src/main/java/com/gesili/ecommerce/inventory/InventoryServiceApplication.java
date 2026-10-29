package com.gesili.ecommerce.inventory;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Inventory service: authoritative for stock availability and reservations.
 *
 * <p>Prevents overselling via locking, holds reservations with a TTL (default 20 minutes —
 * longer than the order payment deadline of 15 minutes), expires stale reservations with a
 * scheduled job, and makes releases idempotent.
 *
 * <p>{@link EnableSchedulerLock} + ShedLock guarantee the expiry job runs on only one
 * instance at a time.
 */
@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
