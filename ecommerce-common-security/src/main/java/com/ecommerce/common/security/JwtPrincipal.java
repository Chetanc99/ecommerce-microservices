package com.ecommerce.common.security;

import java.util.List;

public record JwtPrincipal(
        Long userId,
        String email,
        List<String> roles
) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
