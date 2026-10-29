package com.gesili.ecommerce.inventory.repository;

import com.gesili.ecommerce.common.enums.ReservationStatus;
import com.gesili.ecommerce.inventory.entity.Reservation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);

    List<Reservation> findByOrderId(UUID orderId);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);
}
