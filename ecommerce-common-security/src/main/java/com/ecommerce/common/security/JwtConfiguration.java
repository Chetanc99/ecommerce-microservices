package com.ecommerce.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfiguration {

    @Bean
    public JwtService jwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer:ecommerce-auth}") String issuer,
            @Value("${jwt.audience:ecommerce-api}") String audience,
            @Value("${jwt.expiration-ms:3600000}") long expirationMillis) {

        return new JwtService(secret, issuer, audience, expirationMillis);
    }
}
