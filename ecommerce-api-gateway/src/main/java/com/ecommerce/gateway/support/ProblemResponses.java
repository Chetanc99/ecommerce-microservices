package com.ecommerce.gateway.support;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public final class ProblemResponses {

    private ProblemResponses() {
    }

    public static Mono<Void> write(
            ServerWebExchange exchange,
            HttpStatus status,
            String title,
            String detail) {

        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.empty();
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        String body = """
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s"}
                """.formatted(
                escape(title),
                status.value(),
                escape(detail),
                escape(exchange.getRequest().getPath().value())
        );

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
