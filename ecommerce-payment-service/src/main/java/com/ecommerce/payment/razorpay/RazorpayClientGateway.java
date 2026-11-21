package com.ecommerce.payment.razorpay;

import com.ecommerce.payment.config.RazorpayProperties;
import com.ecommerce.payment.exception.PaymentConfigurationException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class RazorpayClientGateway implements RazorpayGateway {

    private final RazorpayProperties properties;
    private final RazorpayClient client;

    public RazorpayClientGateway(RazorpayProperties properties) {

        this.properties = properties;
        this.client = createClient(properties);
    }

    @Override
    public RazorpayOrderResult createOrder(
            BigDecimal amount,
            String receipt,
            Map<String, String> notes) {

        if (client == null) {
            throw new PaymentConfigurationException(
                    "Razorpay test credentials are not configured"
            );
        }

        try {
            JSONObject notesJson = new JSONObject();
            notes.forEach(notesJson::put);

            JSONObject request = new JSONObject();
            request.put("amount", toPaise(amount));
            request.put("currency", properties.currency());
            request.put("receipt", receipt);
            request.put("notes", notesJson);

            Order order = client.orders.create(request);

            return new RazorpayOrderResult(
                    order.get("id").toString(),
                    amount,
                    properties.currency()
            );
        } catch (RazorpayException exception) {
            throw new PaymentConfigurationException(
                    "Razorpay order creation failed: " + exception.getMessage()
            );
        }
    }

    private RazorpayClient createClient(RazorpayProperties properties) {

        if (!properties.isConfigured()) {
            return null;
        }

        try {
            return new RazorpayClient(properties.keyId(), properties.keySecret());
        } catch (RazorpayException exception) {
            throw new IllegalStateException("Invalid Razorpay credentials", exception);
        }
    }

    private int toPaise(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .intValueExact();
    }
}
