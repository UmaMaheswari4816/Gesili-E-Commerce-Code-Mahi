package com.gecili.ecommerce.inventory.controller;

import com.gecili.ecommerce.common.constant.ApiConstants;
import com.gecili.ecommerce.inventory.dto.InventoryDtos.ReservationResponse;
import com.gecili.ecommerce.inventory.dto.InventoryDtos.ReserveRequest;
import com.gecili.ecommerce.inventory.dto.InventoryDtos.StockResponse;
import com.gecili.ecommerce.inventory.dto.InventoryDtos.UpsertStockRequest;
import com.gecili.ecommerce.inventory.service.InventoryService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inventory API. Stock administration is admin-only; reservation operations are internal
 * (called by the order saga in Phase 6; secured as internal/service callers). In Phase 5 the
 * reserve/release/commit endpoints are exposed for testing and documented as internal.
 */
@RestController
@RequestMapping(ApiConstants.API_V1 + "/inventory")
@Tag(name = "Inventory", description = "Stock and reservations")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @Operation(summary = "Get stock for a SKU")
    @GetMapping("/stock/{sku}")
    public StockResponse getStock(@PathVariable String sku) {
        return inventory.getBySku(sku);
    }

    @Operation(summary = "Upsert absolute available stock for a SKU (admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock set"),
        @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @PutMapping("/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public StockResponse upsertStock(@Valid @RequestBody UpsertStockRequest req) {
        return inventory.upsertStock(req);
    }

    @Operation(summary = "Reserve stock for an order (idempotent on orderId:sku)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reserved (or existing reservation)"),
        @ApiResponse(responseCode = "404", description = "SKU not stocked"),
        @ApiResponse(responseCode = "409", description = "Insufficient stock")
    })
    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventory.reserve(req));
    }

    @Operation(summary = "Release a reservation (idempotent)")
    @PostMapping("/reservations/{id}/release")
    public ResponseEntity<Void> release(@PathVariable UUID id) {
        inventory.release(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Commit a reservation (payment captured)")
    @PostMapping("/reservations/{id}/commit")
    public ResponseEntity<Void> commit(@PathVariable UUID id) {
        inventory.commit(id);
        return ResponseEntity.noContent().build();
    }
}
