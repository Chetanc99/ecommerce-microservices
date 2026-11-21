package com.ecommerce.gateway.filter;

import com.ecommerce.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationGatewayFilterTest {

    private static final String SECRET = "test-only-jwt-secret-key-at-least-32-bytes-long";

    private final JwtService jwtService =
            new JwtService(SECRET, "ecommerce-auth", "ecommerce-api", 3_600_000);

    private final JwtAuthenticationGatewayFilter filter =
            new JwtAuthenticationGatewayFilter(jwtService);

    @Test
    void rejectsProtectedRouteWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders").build());

        filter.filter(exchange, noOpChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("bearer token is required");
    }

    @Test
    void rejectsMalformedToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token")
                        .build());

        filter.filter(exchange, noOpChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("invalid or has expired");
    }

    @Test
    void forwardsIdentityHeadersForValidToken() {
        String token = jwtService.generateToken(7L, "buyer@example.com", List.of("USER", "ADMIN"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(JwtAuthenticationGatewayFilter.USER_ID_HEADER, "999")
                        .build());

        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();

        assertThat(headers.getFirst(JwtAuthenticationGatewayFilter.USER_ID_HEADER)).isEqualTo("7");
        assertThat(headers.getFirst(JwtAuthenticationGatewayFilter.USER_EMAIL_HEADER))
                .isEqualTo("buyer@example.com");
        assertThat(headers.getFirst(JwtAuthenticationGatewayFilter.USER_ROLES_HEADER))
                .isEqualTo("USER,ADMIN");
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
    }

    @Test
    void publicRouteBypassesTokenCheckAndStripsSpoofedIdentity() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products/1")
                        .header(JwtAuthenticationGatewayFilter.USER_ID_HEADER, "999")
                        .header(JwtAuthenticationGatewayFilter.USER_ROLES_HEADER, "ADMIN")
                        .build());

        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();

        assertThat(headers.getFirst(JwtAuthenticationGatewayFilter.USER_ID_HEADER)).isNull();
        assertThat(headers.getFirst(JwtAuthenticationGatewayFilter.USER_ROLES_HEADER)).isNull();
    }

    private GatewayFilterChain noOpChain() {
        return exchange -> Mono.empty();
    }
}
