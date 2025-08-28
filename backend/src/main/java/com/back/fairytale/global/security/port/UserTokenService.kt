package com.back.fairytale.global.security.port

interface UserTokenService {
    fun getUserToken(userId: Long): String
}
