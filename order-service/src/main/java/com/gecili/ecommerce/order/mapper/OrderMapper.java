package com.gecili.ecommerce.order.mapper;

import com.gecili.ecommerce.order.dto.OrderDtos.OrderLineResponse;
import com.gecili.ecommerce.order.dto.OrderDtos.OrderResponse;
import com.gecili.ecommerce.order.entity.OrderEntity;
import org.springframework.stereotype.Component;

/**
 * Maps the order aggregate to its response DTO. Hand-written (not MapStruct) because the
 * status enum -> String and the conditional checkout params are simplest expressed directly.
 * Checkout params are populated in Phase 7 (payment); null until then.
 */
@Component
public class OrderMapper {

    public OrderResponse toResponse(OrderEntity o) {
        var lines =
                o.getItems().stream()
                        .map(i -> new OrderLineResponse(i.getProductId(), i.getSku(), i.getQuantity(), i.getUnitPrice()))
                        .toList();
        return new OrderResponse(
                o.getId(),
                o.getUserId(),
                o.getStatus().name(),
                o.getTotalAmount(),
                o.getCurrency(),
                o.getPaymentDeadline(),
                o.getCancelReason(),
                lines,
                null);
    }
}
