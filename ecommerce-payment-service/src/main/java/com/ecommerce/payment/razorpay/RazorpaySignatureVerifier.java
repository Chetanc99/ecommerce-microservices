package com.ecommerce.payment.razorpay;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class RazorpaySignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";

    public boolean verifyPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String signature,
            String keySecret) {

        return verify(razorpayOrderId + "|" + razorpayPaymentId, signature, keySecret);
    }

    public boolean verifyWebhook(String payload, String signature, String webhookSecret) {
        return verify(payload, signature, webhookSecret);
    }

    private boolean verify(String payload, String signature, String secret) {

        if (isBlank(payload) || isBlank(signature) || isBlank(secret)) {
            return false;
        }

        byte[] expected = hmac(payload, secret).getBytes(StandardCharsets.UTF_8);
        byte[] provided = signature.toLowerCase().getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expected, provided);
    }

    private String hmac(String payload, String secret) {

        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not verify Razorpay signature", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
