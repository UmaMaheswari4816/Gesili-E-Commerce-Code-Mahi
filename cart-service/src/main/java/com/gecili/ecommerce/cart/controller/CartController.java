package com.gecili.ecommerce.cart.controller;

import com.gecili.ecommerce.cart.dto.CartDtos.AddItemRequest;
import com.gecili.ecommerce.cart.dto.CartDtos.CartResponse;
import com.gecili.ecommerce.cart.dto.CartDtos.UpdateQuantityRequest;
import com.gecili.ecommerce.cart.service.CartService;
import com.gecili.ecommerce.common.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cart API. The cart belongs to the authenticated user — the user id comes from the JWT
 * subject, never from the client, so one user can't read or mutate another's cart.
 */
@RestController
@RequestMapping(ApiConstants.API_V1 + "/cart")
@Tag(name = "Cart", description = "User shopping cart (Redis-backed)")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cart;
    private final com.gecili.ecommerce.cart.service.ProductLookupService productLookup;

    public CartController(
            CartService cart, com.gecili.ecommerce.cart.service.ProductLookupService productLookup) {
        this.cart = cart;
        this.productLookup = productLookup;
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @Operation(summary = "Get the current user's cart")
    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal Jwt jwt) {
        return cart.get(userId(jwt));
    }

    @Operation(summary = "Add an item to the cart")
    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddItemRequest req) {
        return cart.addItem(userId(jwt), req);
    }

    @Operation(summary = "Update the quantity of a line (0 removes it)")
    @PutMapping("/items/{sku}")
    public CartResponse updateQuantity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String sku,
            @Valid @RequestBody UpdateQuantityRequest req) {
        return cart.updateQuantity(userId(jwt), sku, req.quantity());
    }

    @Operation(summary = "Remove a line from the cart")
    @DeleteMapping("/items/{sku}")
    public CartResponse removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable String sku) {
        return cart.removeItem(userId(jwt), sku);
    }

    @Operation(summary = "Clear the cart")
    @DeleteMapping
    public ResponseEntity<Void> clear(@AuthenticationPrincipal Jwt jwt) {
        cart.clear(userId(jwt));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Look up live product details (Resilience4j demo: CB + retry + bulkhead + timeout)")
    @GetMapping("/product-info/{productId}")
    public ResponseEntity<?> productInfo(@PathVariable java.util.UUID productId) throws Exception {
        var result = productLookup.getProduct(productId).get();
        return result
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(
                        () ->
                                ResponseEntity.status(503)
                                        .body("product-service unavailable (degraded); try again shortly"));
    }
}
