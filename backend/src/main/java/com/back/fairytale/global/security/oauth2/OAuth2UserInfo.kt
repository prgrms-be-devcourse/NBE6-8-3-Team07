package com.back.fairytale.global.security.oauth2

data class OAuth2UserInfo(
    val id: Long,
    val socialId: String,
    val name: String,
    val nickname: String,
    val email: String,
    val role: String
)
