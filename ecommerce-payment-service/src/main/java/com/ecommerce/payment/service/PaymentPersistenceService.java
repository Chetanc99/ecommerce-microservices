package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentOutboxEvent;
import com.ecommerce.payment.outbox.OutboxMessage;
import com.ecommerce.payment.outbox.PaymentEventFactory;
import com.ecommerce.payment.repository.PaymentOutboxRepository;
import com.ecommerce.payment.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentPersistenceService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final PaymentEventFactory eventFactory;

    public PaymentPersistenceService(
            PaymentRepository paymentRepository,
            PaymentOutboxRepository outboxRepository,
            PaymentEventFactory eventFactory) {

        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.eventFactory = eventFactory;
    }

    @Transactional
    public Payment createPayment(Payment payment) {

        Payment savedPayment = paymentRepository.save(payment);
        append(outboxRepository, eventFactory.paymentInitiated(savedPayment));

        return savedPayment;
    }

    @Transactional
    public Payment saveWithEvent(Payment payment, OutboxMessage message) {

        Payment savedPayment = paymentRepository.save(payment);
        append(outboxRepository, message);

        return savedPayment;
    }

    public void appendEvent(OutboxMessage message) {
        append(outboxRepository, message);
    }

    private void append(PaymentOutboxRepository repository, OutboxMessage message) {

        PaymentOutboxEvent outboxEvent = new PaymentOutboxEvent();
        outboxEvent.setEventId(message.eventId());
        outboxEvent.setEventType(message.eventType());
        outboxEvent.setTopic(message.topic());
        outboxEvent.setPayload(message.payload());

        repository.save(outboxEvent);
    }
}
