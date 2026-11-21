package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, String> {
}
