package com.back.fairytale.domain.user.dto

data class TokenPairDto(
    val accessToken: String, val refreshToken: String
)
