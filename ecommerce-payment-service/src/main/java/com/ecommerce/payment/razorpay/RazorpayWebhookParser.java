package com.ecommerce.payment.razorpay;

import com.ecommerce.payment.exception.PaymentVerificationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class RazorpayWebhookParser {

    public RazorpayWebhookEvent parse(String rawBody) {

        try {
            JSONObject body = new JSONObject(rawBody);
            String event = body.getString("event");
            JSONObject payload = body.getJSONObject("payload");

            JSONObject entity = payload.has("payment")
                    ? payload.getJSONObject("payment").getJSONObject("entity")
                    : payload.getJSONObject("order").getJSONObject("entity");

            return new RazorpayWebhookEvent(
                    event,
                    entity.optString("order_id", entity.optString("id", null)),
                    entity.optString("id", null),
                    entity.optString("error_description", null)
            );
        } catch (JSONException exception) {
            throw new PaymentVerificationException("Malformed Razorpay webhook payload");
        }
    }
}
