package com.ecommerce.order.client;

import com.ecommerce.order.dto.ProductResponse;
import com.ecommerce.order.dto.StockRequest;
import com.ecommerce.order.exception.InsufficientStockException;
import com.ecommerce.order.exception.ProductServiceUnavailableException;
import com.ecommerce.order.exception.ProductUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public ProductClient(
            RestClient productRestClient,
            @Value("${internal.api-key}") String internalApiKey) {

        this.restClient = productRestClient;
        this.internalApiKey = internalApiKey;
    }

    public ProductResponse getProductById(Long productId) {

        try {
            return restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ProductUnavailableException("Product " + productId + " is not available");
            }
            throw new ProductServiceUnavailableException("Product lookup failed", exception);
        } catch (RuntimeException exception) {
            throw new ProductServiceUnavailableException("Product service is unavailable", exception);
        }
    }

    public void reserveStock(Long productId, int quantity) {

        try {
            restClient.post()
                    .uri("/internal/products/{id}/reserve", productId)
                    .header(INTERNAL_KEY_HEADER, internalApiKey)
                    .body(new StockRequest(quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.CONFLICT) {
                throw new InsufficientStockException(
                        "Product " + productId + " does not have enough stock"
                );
            }
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ProductUnavailableException("Product " + productId + " is not available");
            }
            throw new ProductServiceUnavailableException("Stock reservation failed", exception);
        } catch (RuntimeException exception) {
            throw new ProductServiceUnavailableException("Product service is unavailable", exception);
        }
    }

    public void releaseStock(Long productId, int quantity) {

        try {
            restClient.post()
                    .uri("/internal/products/{id}/release", productId)
                    .header(INTERNAL_KEY_HEADER, internalApiKey)
                    .body(new StockRequest(quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Cannot release stock for missing product {}", productId);
                return;
            }
            throw new ProductServiceUnavailableException("Stock release failed", exception);
        } catch (RuntimeException exception) {
            throw new ProductServiceUnavailableException("Product service is unavailable", exception);
        }
    }
}
