package com.ecommerce.notification.messaging;

import com.ecommerce.common.events.KafkaTopics;
import com.ecommerce.common.events.OrderCancelledEvent;
import com.ecommerce.common.events.OrderConfirmedEvent;
import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentSuccessfulEvent;
import com.ecommerce.notification.email.NotificationMessage;
import com.ecommerce.notification.service.NotificationService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    public NotificationEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CONFIRMED,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderConfirmed(OrderConfirmedEvent event) {

        notificationService.deliver(new NotificationMessage(
                event.eventId(),
                "ORDER_CONFIRMED",
                event.userEmail(),
                "Order #" + event.orderId() + " confirmed",
                """
                        Hello,

                        Your order #%d has been confirmed.
                        Order total: INR %s

                        Thank you for shopping with us.
                        """.formatted(event.orderId(), event.totalAmount())
        ));
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_SUCCESSFUL,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentSuccessful(PaymentSuccessfulEvent event) {

        notificationService.deliver(new NotificationMessage(
                event.eventId(),
                "PAYMENT_SUCCESSFUL",
                event.userEmail(),
                "Payment received for order #" + event.orderId(),
                """
                        Hello,

                        We received your payment of INR %s for order #%d.
                        Razorpay payment reference: %s

                        Thank you for shopping with us.
                        """.formatted(event.amount(), event.orderId(), event.razorpayPaymentId())
        ));
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentFailed(PaymentFailedEvent event) {

        notificationService.deliver(new NotificationMessage(
                event.eventId(),
                "PAYMENT_FAILED",
                event.userEmail(),
                "Payment failed for order #" + event.orderId(),
                """
                        Hello,

                        Your payment of INR %s for order #%d could not be completed.
                        Reason: %s

                        You can retry checkout from your order history.
                        """.formatted(event.amount(), event.orderId(), event.reason())
        ));
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderCancelled(OrderCancelledEvent event) {

        notificationService.deliver(new NotificationMessage(
                event.eventId(),
                "ORDER_CANCELLED",
                event.userEmail(),
                "Order #" + event.orderId() + " cancelled",
                """
                        Hello,

                        Your order #%d (INR %s) has been cancelled.
                        Reason: %s

                        If a payment was captured, it will be reviewed for refund.
                        """.formatted(event.orderId(), event.totalAmount(), event.reason())
        ));
    }
}
