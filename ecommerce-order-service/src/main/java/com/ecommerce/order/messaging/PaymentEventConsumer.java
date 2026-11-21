package com.ecommerce.order.messaging;

import com.ecommerce.common.events.KafkaTopics;
import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentSuccessfulEvent;
import com.ecommerce.order.service.OrderPaymentEventHandler;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private final OrderPaymentEventHandler eventHandler;

    public PaymentEventConsumer(OrderPaymentEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_SUCCESSFUL, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentSuccessful(PaymentSuccessfulEvent event) {
        eventHandler.handlePaymentSuccessful(event);
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentFailed(PaymentFailedEvent event) {
        eventHandler.handlePaymentFailed(event);
    }
}
