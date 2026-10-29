package com.gesili.ecommerce.payment.repository;

import com.gesili.ecommerce.payment.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {}
