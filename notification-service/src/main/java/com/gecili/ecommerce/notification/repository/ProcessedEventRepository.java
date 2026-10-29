package com.gecili.ecommerce.notification.repository;

import com.gecili.ecommerce.notification.entity.ProcessedEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {}
