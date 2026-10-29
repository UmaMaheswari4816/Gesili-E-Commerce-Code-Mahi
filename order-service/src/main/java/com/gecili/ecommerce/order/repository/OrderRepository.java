package com.gecili.ecommerce.order.repository;

import com.gecili.ecommerce.common.enums.OrderStatus;
import com.gecili.ecommerce.order.entity.OrderEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findByUserId(UUID userId);

    /** Overdue orders still awaiting payment — used by the payment-timeout sweep. */
    List<OrderEntity> findByStatusAndPaymentDeadlineBefore(OrderStatus status, Instant cutoff);
}
