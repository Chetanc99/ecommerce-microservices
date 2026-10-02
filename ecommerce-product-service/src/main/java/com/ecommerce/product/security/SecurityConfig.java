package com.ecommerce.product.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;


@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService) {

        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Public GET APIs
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/products/**"
                ).permitAll()

                // JWT required
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/products"
                ).authenticated()

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/products/**"
                ).authenticated()

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/products/**"
                ).authenticated()

                .anyRequest().authenticated()
            )

            // IMPORTANT: unauthenticated request -> 401
            .exceptionHandling(exception ->
                exception.authenticationEntryPoint(
                    (request, response, authException) -> {
                        response.setStatus(
                            HttpServletResponse.SC_UNAUTHORIZED
                        );
                    }
                )
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}