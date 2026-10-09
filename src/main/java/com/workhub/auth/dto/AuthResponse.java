package com.workhub.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserDto user
) {
    public record UserDto(
            UUID id,
            String email,
            String displayName
    ) {}
}
