package com.gesili.ecommerce.inventory.service;

import com.gesili.ecommerce.common.enums.ReservationStatus;
import com.gesili.ecommerce.inventory.entity.Reservation;
import com.gesili.ecommerce.inventory.repository.ReservationRepository;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sweeps {@code HELD} reservations past their TTL and releases them, returning stock to
 * available. Wrapped with ShedLock so it runs on only one instance at a time (no
 * double-release across a cluster).
 *
 * <p>In Phase 6 this job also publishes an {@code InventoryReleased} event per expired
 * reservation; here it performs the local release. Each release goes through
 * {@link InventoryService#release} which is idempotent and status-guarded.
 */
@Slf4j
@Component
public class ReservationExpiryJob {

    private final ReservationRepository reservations;
    private final InventoryService inventoryService;

    public ReservationExpiryJob(
            ReservationRepository reservations, InventoryService inventoryService) {
        this.reservations = reservations;
        this.inventoryService = inventoryService;
    }

    @Scheduled(fixedDelayString = "${gesili.inventory.expiry-scan-ms:60000}")
    @SchedulerLock(name = "reservation-expiry", lockAtLeastFor = "PT10S", lockAtMostFor = "PT2M")
    public void expireStaleReservations() {
        List<Reservation> expired =
                reservations.findByStatusAndExpiresAtBefore(ReservationStatus.HELD, Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        log.info("Expiring {} stale reservation(s)", expired.size());
        for (Reservation r : expired) {
            try {
                inventoryService.release(r.getId());
            } catch (Exception e) {
                log.error("Failed to expire reservation {}: {}", r.getId(), e.getMessage());
            }
        }
    }
}
