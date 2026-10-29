package com.gesili.ecommerce.order.controller;

import com.gesili.ecommerce.common.constant.ApiConstants;
import com.gesili.ecommerce.order.dto.OrderDtos.OrderResponse;
import com.gesili.ecommerce.order.dto.OrderDtos.PlaceOrderRequest;
import com.gesili.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order API. Place-order returns 202 with the order in PENDING_INVENTORY; the client then
 * polls GET /orders/{id} until status is PAYMENT_PENDING (checkout params appear) or a
 * terminal state.
 *
 * <p>FLOW (where this sits):
 * <pre>
 *   browser → API gateway (:8080) → HERE (OrderController :9005)
 *       POST /orders  → OrderService.placeOrder()  → saves order + outbox row
 *       GET  /orders/{id}  → OrderService.get()     ← the client polls this
 * </pre>
 * This is the ENTRY POINT of the order saga. From here the flow goes asynchronous: the
 * outbox relay publishes OrderCreated to Kafka, and the saga continues in the listeners
 * (see {@code SagaEventListener}). Nothing here calls inventory/payment directly.
 */
@RestController
@RequestMapping(ApiConstants.API_V1 + "/orders")
@Tag(name = "Orders", description = "Order lifecycle (choreography saga)")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @Operation(summary = "Place an order (async saga; returns 202 with PENDING_INVENTORY)")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Order accepted; saga started"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> place(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlaceOrderRequest req) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(orders.placeOrder(userId(jwt), req));
    }

    @Operation(summary = "Get an order (poll this for status + checkout params)")
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return orders.get(id);
    }

    @Operation(summary = "List my orders")
    @GetMapping
    public List<OrderResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return orders.listForUser(userId(jwt));
    }
}
