package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.support.ProblemResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

@Component
public class GatewayErrorFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return chain.filter(exchange).onErrorResume(exception -> {

            if (exchange.getResponse().isCommitted()) {
                return Mono.error(exception);
            }

            if (exception instanceof ResponseStatusException statusException) {
                HttpStatus status = HttpStatus.resolve(statusException.getStatusCode().value());

                return ProblemResponses.write(exchange,
                        status == null ? HttpStatus.BAD_GATEWAY : status,
                        "Gateway request failed",
                        statusException.getReason() == null
                                ? "The request could not be completed."
                                : statusException.getReason());
            }

            if (isConnectionFailure(exception)) {
                log.warn("Downstream service unavailable for {}: {}",
                        exchange.getRequest().getPath().value(), exception.getMessage());

                return ProblemResponses.write(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                        "Service unavailable",
                        "A downstream service is currently unavailable. Please retry shortly.");
            }

            log.error("Unhandled gateway error for {}",
                    exchange.getRequest().getPath().value(), exception);

            return ProblemResponses.write(exchange, HttpStatus.BAD_GATEWAY,
                    "Bad gateway",
                    "The gateway could not complete the request.");
        });
    }

    private boolean isConnectionFailure(Throwable exception) {

        Throwable current = exception;

        while (current != null) {
            if (current instanceof ConnectException) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
