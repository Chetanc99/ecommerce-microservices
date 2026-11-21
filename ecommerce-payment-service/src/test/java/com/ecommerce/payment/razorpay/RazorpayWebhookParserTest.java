package com.ecommerce.payment.razorpay;

import com.ecommerce.payment.exception.PaymentVerificationException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RazorpayWebhookParserTest {

    private final RazorpayWebhookParser parser = new RazorpayWebhookParser();

    @Test
    void parsesCapturedPaymentEntity() {
        RazorpayWebhookEvent event = parser.parse("""
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_XYZ789",
                        "order_id": "order_ABC123",
                        "status": "captured"
                      }
                    }
                  }
                }
                """);

        assertThat(event.event()).isEqualTo("payment.captured");
        assertThat(event.razorpayOrderId()).isEqualTo("order_ABC123");
        assertThat(event.razorpayPaymentId()).isEqualTo("pay_XYZ789");
        assertThat(event.failureReason()).isNull();
    }

    @Test
    void parsesFailedPaymentReason() {
        RazorpayWebhookEvent event = parser.parse("""
                {
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_FAILED",
                        "order_id": "order_ABC123",
                        "error_description": "Card declined"
                      }
                    }
                  }
                }
                """);

        assertThat(event.event()).isEqualTo("payment.failed");
        assertThat(event.razorpayPaymentId()).isEqualTo("pay_FAILED");
        assertThat(event.failureReason()).isEqualTo("Card declined");
    }

    @Test
    void usesThePaymentEntityWhenOrderPaidIncludesBothEntities() {
        RazorpayWebhookEvent event = parser.parse("""
                {
                  "event": "order.paid",
                  "payload": {
                    "order": {
                      "entity": {"id": "order_ABC123", "status": "paid"}
                    },
                    "payment": {
                      "entity": {"id": "pay_XYZ789", "order_id": "order_ABC123"}
                    }
                  }
                }
                """);

        assertThat(event.razorpayOrderId()).isEqualTo("order_ABC123");
        assertThat(event.razorpayPaymentId()).isEqualTo("pay_XYZ789");
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> parser.parse("{not-json"))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessage("Malformed Razorpay webhook payload");
    }

    @Test
    void rejectsPayloadsWithoutAnEventName() {
        assertThatThrownBy(() -> parser.parse("""
                {"payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_1"}}}}
                """))
                .isInstanceOf(PaymentVerificationException.class);
    }

    @Test
    void rejectsPayloadsWithoutAnEntity() {
        assertThatThrownBy(() -> parser.parse("""
                {"event":"payment.captured","payload":{}}
                """))
                .isInstanceOf(PaymentVerificationException.class);
    }
}
