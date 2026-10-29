package com.gesili.ecommerce.inventory.service;

import com.gesili.ecommerce.common.enums.ReservationStatus;
import com.gesili.ecommerce.common.exception.InsufficientStockException;
import com.gesili.ecommerce.common.exception.ResourceNotFoundException;
import com.gesili.ecommerce.inventory.config.InventoryProperties;
import com.gesili.ecommerce.inventory.dto.InventoryDtos.ReservationResponse;
import com.gesili.ecommerce.inventory.dto.InventoryDtos.ReserveRequest;
import com.gesili.ecommerce.inventory.dto.InventoryDtos.StockResponse;
import com.gesili.ecommerce.inventory.dto.InventoryDtos.UpsertStockRequest;
import com.gesili.ecommerce.inventory.entity.Reservation;
import com.gesili.ecommerce.inventory.entity.StockItem;
import com.gesili.ecommerce.inventory.repository.ReservationRepository;
import com.gesili.ecommerce.inventory.repository.StockItemRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core inventory logic: no overselling, idempotent reservations, TTL, idempotent releases,
 * and commit.
 *
 * <p><strong>No overselling</strong>: the reserve path locks the stock row with
 * {@code SELECT ... FOR UPDATE} (pessimistic) so concurrent buyers are serialized; the
 * {@code @Version} column adds optimistic protection for other paths. The availability check
 * happens while the row is locked, so two requests can never both pass it.
 *
 * <p><strong>Idempotent reserve</strong>: keyed on {@code orderId:sku}. A duplicate reserve
 * (e.g. a redelivered Kafka message) returns the existing reservation instead of holding
 * twice.
 *
 * <p><strong>Idempotent release</strong>: guarded by status. Only {@code HELD -> RELEASED}
 * returns stock; a release of an already {@code RELEASED} or {@code COMMITTED} reservation is
 * a no-op that still succeeds and never re-adds stock.
 *
 * <p>FLOW (who calls what):
 * <pre>
 *   reserve()  ← InventorySagaListener (on OrderCreated)   — the no-oversell hot path
 *   commit()   ← InventorySagaListener (on PaymentCompleted)
 *   release()  ← InventorySagaListener (on PaymentFailed/OrderCancelled)
 *              ← ReservationExpiryJob (TTL expired)
 *   upsertStock() ← InventoryController (admin sets stock levels)
 * </pre>
 */
@Slf4j
@Service
public class InventoryService {

    private final StockItemRepository stock;
    private final ReservationRepository reservations;
    private final InventoryProperties props;

    public InventoryService(
            StockItemRepository stock,
            ReservationRepository reservations,
            InventoryProperties props) {
        this.stock = stock;
        this.reservations = reservations;
        this.props = props;
    }

    @Transactional
    public StockResponse upsertStock(UpsertStockRequest req) {
        StockItem item =
                stock.findBySku(req.sku())
                        .orElseGet(() -> StockItem.create(req.productId(), req.sku(), 0));
        // Set absolute available (admin correction); keep reserved as-is.
        item.setAvailable(req.available());
        item.setProductId(req.productId());
        stock.save(item);
        return toStockResponse(item);
    }

    @Transactional(readOnly = true)
    public StockResponse getBySku(String sku) {
        return stock.findBySku(sku)
                .map(this::toStockResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("StockItem for sku", sku));
    }

    /**
     * Reserve stock for one SKU of an order. Idempotent on {@code orderId:sku}. Throws
     * {@link InsufficientStockException} if not enough is available.
     */
    @Transactional
    public ReservationResponse reserve(ReserveRequest req) {
        String idemKey = req.orderId() + ":" + req.sku();

        // Idempotency: a duplicate reserve returns the existing reservation unchanged.
        Reservation existing = reservations.findByIdempotencyKey(idemKey).orElse(null);
        if (existing != null) {
            log.debug("Idempotent reserve hit for {}", idemKey);
            return toReservationResponse(existing);
        }

        // Lock the stock row so the availability check + hold are atomic vs. other buyers.
        StockItem item =
                stock.findBySkuForUpdate(req.sku())
                        .orElseThrow(() -> ResourceNotFoundException.of("StockItem for sku", req.sku()));

        if (req.quantity() > item.getAvailable()) {
            throw new InsufficientStockException(
                    "Insufficient stock for " + req.sku() + ": requested " + req.quantity()
                            + ", available " + item.getAvailable());
        }

        item.hold(req.quantity());
        stock.save(item);

        Instant expiresAt = Instant.now().plusSeconds(props.reservationTtlSeconds());
        Reservation reservation =
                Reservation.hold(req.orderId(), req.productId(), req.sku(), req.quantity(), expiresAt);
        reservations.save(reservation);
        log.info("Reserved {} x {} for order {} (expires {})",
                req.quantity(), req.sku(), req.orderId(), expiresAt);
        return toReservationResponse(reservation);
    }

    /**
     * Release a single reservation. Idempotent: only HELD reservations return stock; anything
     * else is a safe no-op.
     *
     * @return true if stock was actually returned, false if it was already released/committed
     */
    @Transactional
    public boolean release(UUID reservationId) {
        Reservation r =
                reservations
                        .findById(reservationId)
                        .orElseThrow(() -> ResourceNotFoundException.of("Reservation", reservationId));
        if (r.getStatus() != ReservationStatus.HELD) {
            log.debug("Release no-op for reservation {} (status {})", reservationId, r.getStatus());
            return false;
        }
        StockItem item =
                stock.findBySkuForUpdate(r.getSku())
                        .orElseThrow(() -> ResourceNotFoundException.of("StockItem for sku", r.getSku()));
        item.release(r.getQuantity());
        stock.save(item);
        r.setStatus(ReservationStatus.RELEASED);
        reservations.save(r);
        log.info("Released reservation {} ({} x {})", reservationId, r.getQuantity(), r.getSku());
        return true;
    }

    /** Commit a reservation (payment captured): stock permanently decremented. */
    @Transactional
    public boolean commit(UUID reservationId) {
        Reservation r =
                reservations
                        .findById(reservationId)
                        .orElseThrow(() -> ResourceNotFoundException.of("Reservation", reservationId));
        if (r.getStatus() != ReservationStatus.HELD) {
            return false;
        }
        StockItem item =
                stock.findBySkuForUpdate(r.getSku())
                        .orElseThrow(() -> ResourceNotFoundException.of("StockItem for sku", r.getSku()));
        item.commit(r.getQuantity());
        stock.save(item);
        r.setStatus(ReservationStatus.COMMITTED);
        reservations.save(r);
        return true;
    }

    private StockResponse toStockResponse(StockItem s) {
        return new StockResponse(s.getId(), s.getProductId(), s.getSku(), s.getAvailable(), s.getReserved());
    }

    private ReservationResponse toReservationResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(), r.getOrderId(), r.getSku(), r.getQuantity(), r.getStatus().name());
    }
}
