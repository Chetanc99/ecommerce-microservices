package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.outbox.PaymentEventFactory;
import com.ecommerce.payment.repository.PaymentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentSettlementService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSettlementService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentPersistenceService persistenceService;
    private final PaymentEventFactory eventFactory;

    public PaymentSettlementService(
            PaymentRepository paymentRepository,
            PaymentPersistenceService persistenceService,
            PaymentEventFactory eventFactory) {

        this.paymentRepository = paymentRepository;
        this.persistenceService = persistenceService;
        this.eventFactory = eventFactory;
    }

    @Transactional
    public Payment markSuccessful(Payment payment, String razorpayPaymentId) {

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }

        if (razorpayPaymentId != null
                && paymentRepository.existsByRazorpayPaymentIdAndIdNot(
                        razorpayPaymentId, payment.getId())) {
            throw new IllegalStateException(
                    "Razorpay payment " + razorpayPaymentId + " is already linked to another payment"
            );
        }

        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.warn("Payment {} was marked failed before a captured webhook", payment.getId());
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setFailureReason(null);
        payment.setStatus(PaymentStatus.SUCCESS);

        Payment savedPayment = paymentRepository.save(payment);
        persistenceService.appendEvent(eventFactory.paymentSuccessful(savedPayment));

        return savedPayment;
    }

    @Transactional
    public Payment markFailed(Payment payment, String reason) {

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Ignoring failure for already successful payment {}", payment.getId());
            return payment;
        }

        if (payment.getStatus() == PaymentStatus.FAILED) {
            return payment;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(truncate(reason));

        Payment savedPayment = paymentRepository.save(payment);
        persistenceService.appendEvent(
                eventFactory.paymentFailed(savedPayment, savedPayment.getFailureReason())
        );

        return savedPayment;
    }

    private String truncate(String reason) {
        if (reason == null) {
            return "Payment failed";
        }
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
