package com.gecili.ecommerce.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gecili.ecommerce.common.exception.InsufficientStockException;
import com.gecili.ecommerce.inventory.dto.InventoryDtos.ReservationResponse;
import com.gecili.ecommerce.inventory.dto.InventoryDtos.ReserveRequest;
import com.gecili.ecommerce.inventory.dto.InventoryDtos.UpsertStockRequest;
import com.gecili.ecommerce.inventory.service.InventoryService;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the inventory invariants against real PostgreSQL (so row locking behaves as in
 * production): no overselling under concurrency, idempotent reserve, idempotent release.
 */
@Testcontainers
@SpringBootTest
class InventoryServiceIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("gecili_inventory");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.config.import", () -> "");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:9001/.well-known/jwks.json");
    }

    @Autowired private InventoryService inventory;

    @Test
    void reserve_isIdempotent_onOrderAndSku() {
        String sku = "SKU-IDEM-001";
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        inventory.upsertStock(new UpsertStockRequest(productId, sku, 10));

        ReservationResponse first = inventory.reserve(new ReserveRequest(orderId, productId, sku, 3));
        ReservationResponse second = inventory.reserve(new ReserveRequest(orderId, productId, sku, 3));

        // Same reservation returned; stock held only once.
        assertThat(second.reservationId()).isEqualTo(first.reservationId());
        assertThat(inventory.getBySku(sku).reserved()).isEqualTo(3);
        assertThat(inventory.getBySku(sku).available()).isEqualTo(7);
    }

    @Test
    void release_isIdempotent_neverDoubleReturnsStock() {
        String sku = "SKU-REL-001";
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        inventory.upsertStock(new UpsertStockRequest(productId, sku, 5));
        var r = inventory.reserve(new ReserveRequest(orderId, productId, sku, 2));

        boolean firstRelease = inventory.release(r.reservationId());
        boolean secondRelease = inventory.release(r.reservationId());

        assertThat(firstRelease).isTrue();
        assertThat(secondRelease).isFalse(); // no-op
        assertThat(inventory.getBySku(sku).available()).isEqualTo(5); // not 7
        assertThat(inventory.getBySku(sku).reserved()).isZero();
    }

    @Test
    void concurrentReservations_doNotOversell() throws Exception {
        String sku = "SKU-CONC-001";
        UUID productId = UUID.randomUUID();
        inventory.upsertStock(new UpsertStockRequest(productId, sku, 10));

        int threads = 20; // 20 buyers each wanting 1, but only 10 in stock
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        var tasks =
                java.util.stream.IntStream.range(0, threads)
                        .<Runnable>mapToObj(
                                i ->
                                        () -> {
                                            try {
                                                inventory.reserve(
                                                        new ReserveRequest(UUID.randomUUID(), productId, sku, 1));
                                                success.incrementAndGet();
                                            } catch (InsufficientStockException e) {
                                                rejected.incrementAndGet();
                                            } catch (Exception ignored) {
                                                // optimistic-lock retries may surface; count as rejected for safety
                                                rejected.incrementAndGet();
                                            }
                                        })
                        .toList();

        var futures = tasks.stream().map(pool::submit).toList();
        for (var f : futures) {
            f.get();
        }
        pool.shutdown();

        // Never oversell: successes cannot exceed stock, and available never goes negative.
        assertThat(success.get()).isLessThanOrEqualTo(10);
        assertThat(inventory.getBySku(sku).available()).isGreaterThanOrEqualTo(0);
        assertThat(inventory.getBySku(sku).reserved()).isEqualTo(success.get());
    }
}
