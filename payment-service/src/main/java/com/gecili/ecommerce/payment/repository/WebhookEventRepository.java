package com.gecili.ecommerce.payment.repository;

import com.gecili.ecommerce.payment.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {}
