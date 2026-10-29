package com.gesili.ecommerce.inventory.dto;

import com.gesili.ecommerce.common.validation.Sku;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** DTOs for stock administration and reservation operations. */
public final class InventoryDtos {

    private InventoryDtos() {}

    @Schema(name = "UpsertStockRequest", description = "Set absolute available quantity for a SKU")
    public record UpsertStockRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull UUID productId,
            @Schema(example = "TSHIRT-RED-XL-001", requiredMode = Schema.RequiredMode.REQUIRED) @Sku String sku,
            @Schema(example = "100", requiredMode = Schema.RequiredMode.REQUIRED) @Min(0) int available) {}

    @Schema(name = "StockResponse")
    public record StockResponse(UUID id, UUID productId, String sku, int available, int reserved) {}

    @Schema(name = "ReserveRequest", description = "Reserve stock for one SKU of an order")
    public record ReserveRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull UUID orderId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull UUID productId,
            @Schema(example = "TSHIRT-RED-XL-001", requiredMode = Schema.RequiredMode.REQUIRED) @Sku String sku,
            @Schema(example = "2", requiredMode = Schema.RequiredMode.REQUIRED) @Min(1) int quantity) {}

    @Schema(name = "ReservationResponse")
    public record ReservationResponse(UUID reservationId, UUID orderId, String sku, int quantity, String status) {}
}
