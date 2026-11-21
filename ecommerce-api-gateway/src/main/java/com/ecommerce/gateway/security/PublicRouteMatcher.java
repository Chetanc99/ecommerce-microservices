package com.ecommerce.gateway.security;

import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.regex.Pattern;

public final class PublicRouteMatcher {

    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute(HttpMethod.POST, Pattern.compile("^/api/users/register$")),
            new PublicRoute(HttpMethod.POST, Pattern.compile("^/api/users/login$")),
            new PublicRoute(HttpMethod.GET, Pattern.compile("^/api/products(/.*)?$")),
            new PublicRoute(HttpMethod.POST, Pattern.compile("^/api/payments/webhook$")),
            new PublicRoute(null, Pattern.compile("^/actuator/(health|info)(/.*)?$"))
    );

    private PublicRouteMatcher() {
    }

    public static boolean isPublic(HttpMethod method, String path) {

        return PUBLIC_ROUTES.stream().anyMatch(route ->
                (route.method() == null || route.method().equals(method))
                        && route.path().matcher(path).matches());
    }

    private record PublicRoute(HttpMethod method, Pattern path) {
    }
}
