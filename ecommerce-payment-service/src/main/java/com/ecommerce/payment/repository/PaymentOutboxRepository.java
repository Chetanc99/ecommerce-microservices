package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.PaymentOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutboxEvent, Long> {

    List<PaymentOutboxEvent> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}
