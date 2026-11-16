package com.commandos.identity.api.dto;

public record AuthSessionResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthUserResponse user) {
}
