package com.gesili.ecommerce.product.dto;

import com.gesili.ecommerce.common.validation.Sku;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** Request/response DTOs for catalog + search, with OpenAPI annotations. */
public final class ProductDtos {

    private ProductDtos() {}

    @Schema(name = "CreateCategoryRequest")
    public record CreateCategoryRequest(
            @Schema(example = "T-Shirts", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String name,
            @Schema(example = "t-shirts", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String slug,
            @Schema(example = "Cotton tees") String description) {}

    @Schema(name = "CategoryResponse")
    public record CategoryResponse(UUID id, String name, String slug, String description) {}

    @Schema(name = "CreateProductRequest")
    public record CreateProductRequest(
            @Schema(example = "TSHIRT-RED-XL-001", requiredMode = Schema.RequiredMode.REQUIRED) @Sku String sku,
            @Schema(example = "Red T-Shirt XL", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 200) String name,
            @Schema(example = "100% cotton, crew neck") String description,
            @Schema(example = "499.00", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @DecimalMin("0.0") BigDecimal price,
            @Schema(example = "INR", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 3, max = 3) String currency,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull UUID categoryId) {}

    @Schema(name = "UpdateProductRequest")
    public record UpdateProductRequest(
            @Schema(example = "Red T-Shirt XL") @Size(max = 200) String name,
            @Schema(example = "Updated description") String description,
            @Schema(example = "549.00") @DecimalMin("0.0") BigDecimal price,
            @Schema(example = "true") Boolean active) {}

    @Schema(name = "ProductResponse")
    public record ProductResponse(
            UUID id,
            String sku,
            String name,
            String description,
            BigDecimal price,
            String currency,
            UUID categoryId,
            boolean active) {}

    @Schema(name = "ProductSearchResult", description = "A product from the search read model")
    public record ProductSearchResult(
            UUID id,
            String sku,
            String name,
            String description,
            BigDecimal price,
            String currency,
            String categoryName,
            String categorySlug) {}
}
