package com.gesili.ecommerce.order.dto;

import com.gesili.ecommerce.common.validation.Sku;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Order request/response DTOs. */
public final class OrderDtos {

    private OrderDtos() {}

    @Schema(name = "PlaceOrderRequest")
    public record PlaceOrderRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty @Valid List<Line> items,
            @Schema(example = "INR", requiredMode = Schema.RequiredMode.REQUIRED) String currency) {

        @Schema(name = "PlaceOrderLine")
        public record Line(
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull UUID productId,
                @Schema(example = "TSHIRT-RED-XL-001", requiredMode = Schema.RequiredMode.REQUIRED) @Sku String sku,
                @Schema(example = "2", requiredMode = Schema.RequiredMode.REQUIRED) @Min(1) int quantity,
                @Schema(example = "499.00", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull BigDecimal unitPrice) {}
    }

    @Schema(name = "OrderLineResponse")
    public record OrderLineResponse(UUID productId, String sku, int quantity, BigDecimal unitPrice) {}

    /**
     * Order view. When {@code status == PAYMENT_PENDING}, {@code checkout} carries the params
     * the client needs to start provider checkout (populated in Phase 7). Clients poll
     * GET /orders/{id} until the checkout params appear.
     */
    @Schema(name = "OrderResponse")
    public record OrderResponse(
            UUID id,
            UUID userId,
            String status,
            BigDecimal totalAmount,
            String currency,
            Instant paymentDeadline,
            String cancelReason,
            List<OrderLineResponse> items,
            CheckoutParams checkout) {}

    @Schema(name = "CheckoutParams", description = "Provider checkout params (present when PAYMENT_PENDING)")
    public record CheckoutParams(String provider, String providerRef, String keyId, BigDecimal amount, String currency) {}
}
