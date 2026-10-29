package com.gesili.ecommerce.inventory.repository;

import com.gesili.ecommerce.inventory.entity.StockItem;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockItemRepository extends JpaRepository<StockItem, UUID> {

    Optional<StockItem> findBySku(String sku);

    /**
     * Pessimistic write lock ({@code SELECT ... FOR UPDATE}) for the hot reserve path.
     * Serializes concurrent reservations of the same SKU at the row level so two buyers can
     * never both pass the availability check and oversell.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockItem s where s.sku = :sku")
    Optional<StockItem> findBySkuForUpdate(@Param("sku") String sku);
}
