package com.gesili.ecommerce.payment.repository;

import com.gesili.ecommerce.common.enums.PaymentStatus;
import com.gesili.ecommerce.payment.entity.Payment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByProviderRef(String providerRef);

    /** Stuck payments the reconciliation job should resolve. */
    List<Payment> findByStatusInAndUpdatedAtBefore(List<PaymentStatus> statuses, Instant cutoff);
}
