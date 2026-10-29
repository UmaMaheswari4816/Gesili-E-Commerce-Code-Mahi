package com.gesili.ecommerce.product.controller;

import com.gesili.ecommerce.common.constant.ApiConstants;
import com.gesili.ecommerce.product.dto.ProductDtos.CategoryResponse;
import com.gesili.ecommerce.product.dto.ProductDtos.CreateCategoryRequest;
import com.gesili.ecommerce.product.dto.ProductDtos.CreateProductRequest;
import com.gesili.ecommerce.product.dto.ProductDtos.ProductResponse;
import com.gesili.ecommerce.product.dto.ProductDtos.UpdateProductRequest;
import com.gesili.ecommerce.product.service.CatalogCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin write endpoints (CQRS command side). Requires ROLE_ADMIN. */
@RestController
@RequestMapping(ApiConstants.API_V1 + "/admin/catalog")
@Tag(name = "Product Catalog (admin)", description = "Manage categories and products")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogAdminController {

    private final CatalogCommandService command;

    public CatalogAdminController(CatalogCommandService command) {
        this.command = command;
    }

    @Operation(summary = "Create a category")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Not an admin"),
        @ApiResponse(responseCode = "409", description = "Duplicate name/slug")
    })
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(command.createCategory(req));
    }

    @Operation(summary = "Create a product")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate SKU")
    })
    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(command.createProduct(req));
    }

    @Operation(summary = "Update a product")
    @PatchMapping("/products/{id}")
    public ProductResponse updateProduct(
            @PathVariable UUID id, @Valid @RequestBody UpdateProductRequest req) {
        return command.updateProduct(id, req);
    }

    @Operation(summary = "Delete a product")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        command.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
