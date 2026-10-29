package com.gecili.ecommerce.order.repository;

import com.gecili.ecommerce.order.entity.ProcessedEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {}
