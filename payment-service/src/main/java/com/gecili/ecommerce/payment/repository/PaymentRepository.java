package com.gecili.ecommerce.payment.repository;

import com.gecili.ecommerce.common.enums.PaymentStatus;
import com.gecili.ecommerce.payment.entity.Payment;
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
