package com.ecommerce.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class PublicRouteMatcherTest {

    @Test
    void registrationAndLoginArePublic() {
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.POST, "/api/users/register")).isTrue();
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.POST, "/api/users/login")).isTrue();
    }

    @Test
    void profileEndpointsAreProtected() {
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.GET, "/api/users/me")).isFalse();
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.PUT, "/api/users/me")).isFalse();
    }

    @Test
    void productReadsArePublicButWritesAreNot() {
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.GET, "/api/products")).isTrue();
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.GET, "/api/products/42")).isTrue();
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.GET, "/api/products/search")).isTrue();
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.POST, "/api/products")).isFalse();
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.PUT, "/api/products/42")).isFalse();
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.DELETE, "/api/products/42")).isFalse();
    }

    @Test
    void ordersAndPaymentsRequireAuthentication() {
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.GET, "/api/orders")).isFalse();
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.POST, "/api/orders")).isFalse();
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.POST, "/api/payments/orders")).isFalse();
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.GET, "/api/payments/order/1")).isFalse();
    }

    @Test
    void razorpayWebhookIsPublic() {
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.POST, "/api/payments/webhook")).isTrue();
        assertThat(PublicRouteMatcher.isPublic(HttpMethod.GET, "/api/payments/webhook")).isFalse();
    }
}
