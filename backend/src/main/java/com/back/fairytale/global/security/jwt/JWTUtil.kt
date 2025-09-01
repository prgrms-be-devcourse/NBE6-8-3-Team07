package com.back.fairytale.global.security.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Component
class JWTUtil(@Value("\${spring.jwt.secret}") secret: String) {
    private val secretKey: SecretKey = SecretKeySpec(
        secret.toByteArray(StandardCharsets.UTF_8),
        Jwts.SIG.HS256.key().build().algorithm
    )

    fun createJwt(userId: Long, role: String, expiredMs: Long, category: String): String {
        return Jwts.builder()
            .claim("userId", userId.toString())
            .claim("role", role)
            .claim("category", category)
            .claim("jti", UUID.randomUUID().toString())
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(Date(System.currentTimeMillis() + expiredMs))
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: JwtException) {
            false
        }
    }

    fun getUserId(token: String): Long {
        val payload = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        return when (val userId = payload["userId"]) {
            is Number -> userId.toLong()
            is String -> userId.toLong()
            else -> throw IllegalArgumentException("Invalid userId type: ${userId?.javaClass}")
        }
    }

    fun getCategory(token: String): String {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .get("category", String::class.java)
    }

    fun getRole(token: String): String {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .get("role", String::class.java)
    }
}
