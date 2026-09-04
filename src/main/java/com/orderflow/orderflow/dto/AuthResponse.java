package com.orderflow.orderflow.dto;

import com.orderflow.orderflow.entity.Role;

public record AuthResponse(
        String token,
        Long userId,
        String name,
        String email,
        Role role) {
}
