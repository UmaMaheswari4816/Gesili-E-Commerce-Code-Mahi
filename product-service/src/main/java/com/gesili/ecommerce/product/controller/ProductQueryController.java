package com.gesili.ecommerce.product.controller;

import com.gesili.ecommerce.common.constant.ApiConstants;
import com.gesili.ecommerce.product.dto.ProductDtos.ProductResponse;
import com.gesili.ecommerce.product.dto.ProductDtos.ProductSearchResult;
import com.gesili.ecommerce.product.service.CatalogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public read endpoints: product lookup and search (CQRS query side). */
@RestController
@RequestMapping(ApiConstants.API_V1 + "/products")
@Tag(name = "Product Catalog (public)", description = "Browse and search the catalog")
public class ProductQueryController {

    private final CatalogQueryService query;

    public ProductQueryController(CatalogQueryService query) {
        this.query = query;
    }

    @Operation(summary = "Get a product by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Found"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        return query.getById(id);
    }

    @Operation(summary = "Search products (Elasticsearch read model)")
    @ApiResponse(responseCode = "200", description = "Search results page")
    @GetMapping("/search")
    public Page<ProductSearchResult> search(
            @Parameter(description = "Free-text query; blank returns all active products", example = "red shirt")
                    @RequestParam(required = false)
                    String q,
            Pageable pageable) {
        return query.search(q, pageable);
    }

    @Operation(summary = "List products in a category")
    @GetMapping("/category/{slug}")
    public Page<ProductSearchResult> byCategory(@PathVariable String slug, Pageable pageable) {
        return query.byCategory(slug, pageable);
    }
}
