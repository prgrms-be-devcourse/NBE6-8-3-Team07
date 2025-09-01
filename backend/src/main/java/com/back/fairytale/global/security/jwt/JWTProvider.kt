package com.back.fairytale.global.security.jwt

import jakarta.servlet.http.Cookie
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JWTProvider(private val jwtUtil: JWTUtil) {

    @Value("\${spring.cookie.secure}")
    private val cookieSecure: Boolean = false

    @Value("\${spring.cookie.same-site:Lax}")
    private val cookieSameSite: String = "Lax"

    enum class TokenType(val tokenName: String, val expirationMs: Long) {
        ACCESS("Authorization", 10 * 60 * 1000L),
        REFRESH("refresh", 24 * 60 * 60 * 1000L);
    }

    fun createRefreshTokenCookie(refreshToken: String): Cookie {
        return createSessionCookie(refreshToken, TokenType.REFRESH.tokenName)
    }

    fun createAccessToken(userId: Long, role: String): String {
        return createToken(userId, role, TokenType.ACCESS)
    }

    fun createRefreshToken(userId: Long, role: String): String {
        return createToken(userId, role, TokenType.REFRESH)
    }

    fun wrapTokenToCookie(token: String, tokenType: TokenType): Cookie {
        return createSessionCookie(token, tokenType.tokenName)
    }

    fun wrapAccessTokenToCookie(token: String): Cookie {
        return wrapTokenToCookie(token, TokenType.ACCESS)
    }

    fun wrapRefreshTokenToCookie(token: String): Cookie {
        return wrapTokenToCookie(token, TokenType.REFRESH)
    }

    fun extractTokenFromCookies(cookies: Array<Cookie>?, tokenType: TokenType): String? {
        return cookies?.find { it.name == tokenType.tokenName }?.value
    }

    fun extractRefreshToken(cookies: Array<Cookie>?): String? {
        return extractTokenFromCookies(cookies, TokenType.REFRESH)
    }

    fun validateAccessToken(accessToken: String?): Boolean {
        return validateToken(accessToken, TokenType.ACCESS)
    }

    fun validateRefreshToken(refreshToken: String?): Boolean {
        return validateToken(refreshToken, TokenType.REFRESH)
    }

    fun getUserIdFromAccessToken(accessToken: String): Long {
        return getUserIdFromToken(accessToken, TokenType.ACCESS)
    }

    fun getUserIdFromRefreshToken(refreshToken: String?): Long {
        return getUserIdFromToken(refreshToken, TokenType.REFRESH)
    }

    fun createSessionCookie(token: String, name: String): Cookie {
        return Cookie(name, token).apply {
            isHttpOnly = true
            path = "/"
            secure = cookieSecure
            setAttribute("SameSite", cookieSameSite)
        }
    }

    fun createCookie(token: String?, name: String, maxAge: Int): Cookie {
        return Cookie(name, token).apply {
            isHttpOnly = true
            path = "/"
            secure = cookieSecure
            this.maxAge = maxAge
            setAttribute("SameSite", cookieSameSite)
        }
    }

    private fun validateToken(token: String?, tokenType: TokenType): Boolean {
        return token?.let {
            jwtUtil.validateToken(it) && tokenType.name == jwtUtil.getCategory(it)
        } ?: false
    }

    private fun getUserIdFromToken(token: String?, tokenType: TokenType): Long {
        require(validateToken(token, tokenType)) { "Invalid ${tokenType.tokenName} token" }
        return jwtUtil.getUserId(token!!)
    }

    private fun createToken(userId: Long, role: String, tokenType: TokenType): String {
        return jwtUtil.createJwt(userId, role, tokenType.expirationMs, tokenType.name)
    }
}