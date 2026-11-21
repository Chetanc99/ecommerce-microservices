package com.ecommerce.gateway.filter;

import com.ecommerce.common.security.JwtPrincipal;
import com.ecommerce.common.security.JwtService;
import com.ecommerce.gateway.security.PublicRouteMatcher;
import com.ecommerce.gateway.support.ProblemResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationGatewayFilter.class);

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    private final JwtService jwtService;

    public JwtAuthenticationGatewayFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (PublicRouteMatcher.isPublic(request.getMethod(), path)) {
            return chain.filter(stripIdentityHeaders(exchange));
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ProblemResponses.write(exchange, HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "A bearer token is required to access this resource.");
        }

        try {
            JwtPrincipal principal = jwtService.parseToken(authorization.substring(7));

            ServerHttpRequest authenticatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.set(USER_ID_HEADER, principal.userId().toString());
                        headers.set(USER_EMAIL_HEADER,
                                principal.email() == null ? "" : principal.email());
                        headers.set(USER_ROLES_HEADER,
                                principal.roles().stream().collect(Collectors.joining(",")));
                    })
                    .build();

            return chain.filter(exchange.mutate().request(authenticatedRequest).build());

        } catch (Exception exception) {
            log.debug("Rejected JWT for {}: {}", path, exception.getMessage());

            return ProblemResponses.write(exchange, HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "The bearer token is invalid or has expired.");
        }
    }

    private ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {

        ServerHttpRequest sanitized = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_EMAIL_HEADER);
                    headers.remove(USER_ROLES_HEADER);
                })
                .build();

        return exchange.mutate().request(sanitized).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 30;
    }
}
