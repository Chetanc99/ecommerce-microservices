package com.ecommerce.order.repository;

import com.ecommerce.order.entity.OrderOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderOutboxRepository extends JpaRepository<OrderOutboxEvent, Long> {

    List<OrderOutboxEvent> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}
