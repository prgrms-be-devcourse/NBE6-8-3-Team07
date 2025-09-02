package com.back.fairytale.global.security.port

interface UserTokenService {
    fun saveOrUpdateUserToken(userId: Long, refreshToken: String)
}
