package com.ecommerce.common.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-only-jwt-secret-key-at-least-32-bytes-long";

    private final JwtService jwtService =
            new JwtService(SECRET, "ecommerce-auth", "ecommerce-api", 3_600_000);

    @Test
    void generatesAndParsesIdentityClaims() {
        String token = jwtService.generateToken(42L, "buyer@example.com", List.of("USER", "ADMIN"));

        JwtPrincipal principal = jwtService.parseToken(token);

        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.email()).isEqualTo("buyer@example.com");
        assertThat(principal.roles()).containsExactly("USER", "ADMIN");
        assertThat(principal.hasRole("ADMIN")).isTrue();
        assertThat(principal.hasRole("SUPPORT")).isFalse();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService otherIssuer = new JwtService(
                "a-completely-different-secret-key-with-32-bytes", "ecommerce-auth",
                "ecommerce-api", 3_600_000);

        String token = otherIssuer.generateToken(42L, "buyer@example.com", List.of("USER"));

        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken(42L, "buyer@example.com", List.of("USER"));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLivedIssuer = new JwtService(
                SECRET, "ecommerce-auth", "ecommerce-api", -1_000);

        String token = shortLivedIssuer.generateToken(42L, "buyer@example.com", List.of("USER"));

        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void refusesSecretsShorterThanHs256Minimum() {
        assertThatThrownBy(() -> new JwtService("too-short", "ecommerce-auth", "ecommerce-api", 1_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
