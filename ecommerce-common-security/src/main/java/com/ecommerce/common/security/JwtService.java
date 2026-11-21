package com.ecommerce.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class JwtService {

    private static final int MINIMUM_KEY_BYTES = 32;

    private final SecretKey signingKey;
    private final String issuer;
    private final String audience;
    private final long expirationMillis;

    public JwtService(
            String secret,
            String issuer,
            String audience,
            long expirationMillis) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < MINIMUM_KEY_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 bytes for HS256 signing"
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.audience = audience;
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(Long userId, String email, List<String> roles) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(signingKey)
                .compact();
    }

    public JwtPrincipal parseToken(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);

        List<?> rawRoles = claims.get("roles", List.class);
        List<String> roles = rawRoles == null
                ? List.of()
                : rawRoles.stream().map(String::valueOf).toList();

        return new JwtPrincipal(userId, email, roles);
    }
}
