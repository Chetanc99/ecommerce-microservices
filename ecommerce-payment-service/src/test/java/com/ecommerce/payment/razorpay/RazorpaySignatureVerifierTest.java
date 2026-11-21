package com.ecommerce.payment.razorpay;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RazorpaySignatureVerifierTest {

    private static final String KEY_SECRET = "test_secret";
    private static final String PAYMENT_SIGNATURE =
            "85cbc6036124891c4d0280fbb7cd83804f87a66f2eb485a89af574086f592cbc";

    private static final String WEBHOOK_SECRET = "webhook_secret";
    private static final String WEBHOOK_PAYLOAD = "{\"event\":\"payment.captured\"}";
    private static final String WEBHOOK_SIGNATURE =
            "63482aecf393ec15e418daab94dffe6cd7a1ddeec5ade268106920e9f1c8363d";

    private final RazorpaySignatureVerifier verifier = new RazorpaySignatureVerifier();

    @Test
    void acceptsAValidPaymentSignature() {
        assertThat(verifier.verifyPayment(
                "order_ABC123", "pay_XYZ789", PAYMENT_SIGNATURE, KEY_SECRET)).isTrue();
    }

    @Test
    void acceptsUppercaseHexSignatures() {
        assertThat(verifier.verifyPayment(
                "order_ABC123", "pay_XYZ789", PAYMENT_SIGNATURE.toUpperCase(), KEY_SECRET))
                .isTrue();
    }

    @Test
    void rejectsATamperedPaymentId() {
        assertThat(verifier.verifyPayment(
                "order_ABC123", "pay_TAMPERED", PAYMENT_SIGNATURE, KEY_SECRET)).isFalse();
    }

    @Test
    void rejectsMissingOrBlankSignatureInputs() {
        assertThat(verifier.verifyPayment("order_ABC123", "pay_XYZ789", null, KEY_SECRET)).isFalse();
        assertThat(verifier.verifyPayment("order_ABC123", "pay_XYZ789", PAYMENT_SIGNATURE, ""))
                .isFalse();
        assertThat(verifier.verifyPayment(null, null, PAYMENT_SIGNATURE, KEY_SECRET)).isFalse();
    }

    @Test
    void acceptsAValidWebhookSignature() {
        assertThat(verifier.verifyWebhook(WEBHOOK_PAYLOAD, WEBHOOK_SIGNATURE, WEBHOOK_SECRET))
                .isTrue();
    }

    @Test
    void rejectsAWebhookPayloadThatWasModifiedAfterSigning() {
        assertThat(verifier.verifyWebhook(
                "{\"event\":\"payment.failed\"}", WEBHOOK_SIGNATURE, WEBHOOK_SECRET)).isFalse();
    }

    @Test
    void rejectsWebhookVerificationWithoutAConfiguredSecret() {
        assertThat(verifier.verifyWebhook(WEBHOOK_PAYLOAD, WEBHOOK_SIGNATURE, null)).isFalse();
    }
}
