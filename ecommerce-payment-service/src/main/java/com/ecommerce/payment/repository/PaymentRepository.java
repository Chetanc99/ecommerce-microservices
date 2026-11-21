package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(
            Long orderId, Collection<PaymentStatus> statuses);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    boolean existsByRazorpayPaymentIdAndIdNot(String razorpayPaymentId, Long id);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
