package com.back.fairytale.domain.user.dto;

public record TokenPairDto(String accessToken, String refreshToken) {

    public static TokenPairDto of(String accessToken, String refreshToken) {
        return new TokenPairDto(accessToken, refreshToken);
    }
}
