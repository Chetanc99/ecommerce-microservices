package com.ecommerce.payment.client;

import com.ecommerce.payment.dto.OrderSummary;
import com.ecommerce.payment.exception.OrderServiceUnavailableException;
import com.ecommerce.payment.exception.PaymentAccessDeniedException;
import com.ecommerce.payment.exception.PaymentNotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OrderClient {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Api-Key";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final RestClient restClient;
    private final String internalApiKey;

    public OrderClient(
            RestClient orderRestClient,
            @Value("${internal.api-key}") String internalApiKey) {

        this.restClient = orderRestClient;
        this.internalApiKey = internalApiKey;
    }

    public OrderSummary getOrderSummary(Long orderId, Long userId) {

        try {
            return restClient.get()
                    .uri("/internal/orders/{orderId}", orderId)
                    .header(INTERNAL_KEY_HEADER, internalApiKey)
                    .header(USER_ID_HEADER, userId.toString())
                    .retrieve()
                    .body(OrderSummary.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new PaymentNotFoundException("Order not found with id: " + orderId);
            }
            if (exception.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new PaymentAccessDeniedException("You cannot pay for this order");
            }
            throw new OrderServiceUnavailableException("Order lookup failed", exception);
        } catch (RuntimeException exception) {
            throw new OrderServiceUnavailableException("Order service is unavailable", exception);
        }
    }
}
