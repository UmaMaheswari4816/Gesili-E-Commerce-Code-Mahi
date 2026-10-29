package com.gesili.ecommerce.cart.service;

import com.gesili.ecommerce.cart.client.ProductClient;
import com.gesili.ecommerce.cart.client.ProductClient.ProductView;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Resilient wrapper around the product-service Feign call, demonstrating the four core
 * Resilience4j patterns together (config in application.yml under {@code resilience4j.*}):
 *
 * <ul>
 *   <li><b>Retry</b> — a transient failure is retried with backoff before giving up.</li>
 *   <li><b>CircuitBreaker</b> — repeated failures "open" the circuit so we fail fast instead
 *       of hammering a sick dependency; it half-opens later to probe recovery.</li>
 *   <li><b>Bulkhead</b> — caps concurrent calls so a slow product-service can't exhaust all
 *       cart-service threads (failure isolation).</li>
 *   <li><b>TimeLimiter</b> — bounds how long we wait, so a hung call doesn't block forever.</li>
 * </ul>
 *
 * On exhaustion/open circuit the {@code fallback} returns {@link Optional#empty()} — the cart
 * still works (it just can't enrich the line with live product data), demonstrating graceful
 * degradation.
 *
 * <p>Order of aspects (R4j default): Retry( CircuitBreaker( RateLimiter( TimeLimiter(
 * Bulkhead( call ))))). TimeLimiter requires a {@code CompletableFuture} return.
 *
 * <p>FLOW (a resilient synchronous cross-service call — contrast with the async saga):
 * <pre>
 *   CartController.productInfo()  → HERE: getProduct()
 *        → ProductClient (OpenFeign) → lb://product-service (resolved via Eureka)
 *        → product-service ProductQueryController.getById()
 *   if product-service is down/slow: retry → circuit opens → fallback() returns empty
 *        → the cart still works (graceful degradation)
 * </pre>
 */
@Slf4j
@Service
public class ProductLookupService {

    private final ProductClient productClient;

    public ProductLookupService(ProductClient productClient) {
        this.productClient = productClient;
    }

    @CircuitBreaker(name = "productLookup", fallbackMethod = "fallback")
    @Retry(name = "productLookup")
    @Bulkhead(name = "productLookup")
    @TimeLimiter(name = "productLookup")
    public CompletableFuture<Optional<ProductView>> getProduct(UUID productId) {
        return CompletableFuture.supplyAsync(() -> Optional.of(productClient.getById(productId)));
    }

    /** Fallback invoked when retries are exhausted, the circuit is open, or a timeout fires. */
    @SuppressWarnings("unused")
    private CompletableFuture<Optional<ProductView>> fallback(UUID productId, Throwable t) {
        log.warn("product-service lookup failed for {} ({}); degrading gracefully", productId, t.toString());
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
