package com.ecommerce.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "razorpay")
public record RazorpayProperties(
        String keyId,
        String keySecret,
        String webhookSecret,
        String currency
) {

    public RazorpayProperties {
        currency = currency == null || currency.isBlank() ? "INR" : currency;
    }

    public boolean isConfigured() {
        return keyId != null && !keyId.isBlank()
                && keySecret != null && !keySecret.isBlank();
    }

    public boolean isWebhookConfigured() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }
}
