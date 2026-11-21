package com.ecommerce.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutingIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void registersOneRoutePerService() {
        StepVerifier.create(routeDefinitionLocator.getRouteDefinitions())
                .expectNextCount(4)
                .verifyComplete();
    }

    @Test
    void protectedRouteWithoutTokenReturnsProblemDetail() {
        client.get().uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader().exists("X-Correlation-Id")
                .expectBody()
                .jsonPath("$.title").isEqualTo("Unauthorized")
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void paymentEndpointRequiresToken() {
        client.post().uri("/api/payments/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"orderId\":1}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void publicProductRouteIsForwardedAndReportsDownstreamFailure() {
        client.get().uri("/api/products/1")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectHeader().exists("X-Correlation-Id")
                .expectBody()
                .jsonPath("$.title").isEqualTo("Service unavailable");
    }

    @Test
    void corsPreflightIsAllowedForTheFrontendOrigin() {
        client.options().uri("/api/orders")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
    }

    @Test
    void actuatorHealthIsPublic() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}
