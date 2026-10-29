package com.gecili.ecommerce.cart.dto;

import com.gecili.ecommerce.common.validation.Sku;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Cart request/response DTOs. */
public final class CartDtos {

    private CartDtos() {}

    @Schema(name = "AddItemRequest")
    public record AddItemRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull UUID productId,
            @Schema(example = "TSHIRT-RED-XL-001", requiredMode = Schema.RequiredMode.REQUIRED) @Sku String sku,
            @Schema(example = "Red T-Shirt XL") String name,
            @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) @Min(1) int quantity,
            @Schema(example = "499.00", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull BigDecimal unitPrice) {}

    @Schema(name = "UpdateQuantityRequest")
    public record UpdateQuantityRequest(
            @Schema(example = "3", requiredMode = Schema.RequiredMode.REQUIRED) @Min(0) int quantity) {}

    @Schema(name = "CartLineResponse")
    public record CartLineResponse(UUID productId, String sku, String name, int quantity, BigDecimal unitPrice) {}

    @Schema(name = "CartResponse")
    public record CartResponse(UUID userId, List<CartLineResponse> lines, BigDecimal total) {}
}
