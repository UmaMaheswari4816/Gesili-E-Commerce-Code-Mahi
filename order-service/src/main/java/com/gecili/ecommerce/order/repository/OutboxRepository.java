package com.gecili.ecommerce.order.repository;

import com.gecili.ecommerce.order.entity.OutboxEvent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Phase 0 Note 1: select unpublished rows oldest-first, NO high-water-mark cursor. The
     * relay publishes these to Kafka keyed by aggregateId, then stamps published_at.
     */
    @Query("select e from OutboxEvent e where e.publishedAt is null order by e.id asc")
    List<OutboxEvent> findUnpublished(Pageable pageable);
}
