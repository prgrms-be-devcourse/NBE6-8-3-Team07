package com.back.fairytale.global.security

import com.back.fairytale.global.security.jwt.JWTUtil
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class JWTUtilTest {
    private lateinit var jwtUtil: JWTUtil
    private val testSecret = "testSecretKeyForJWTTokenGenerationAndValidation1234567890"

    @BeforeEach
    fun setUp() {
        jwtUtil = JWTUtil(testSecret)
    }

    @Test
    @DisplayName("JWT 토큰 생성 테스트")
    fun testCreateJwt() {
        val userId = 1L
        val role = "ROLE_USER"
        val expiredMs = 60000L

        val token = jwtUtil.createJwt(userId, role, expiredMs, "access")

        Assertions.assertThat(token).isNotNull()
        Assertions.assertThat(token.split(".")).hasSize(3)// JWT header.payload.signature 형태
    }

    @Test
    @DisplayName("유효한 JWT 토큰 검증 테스트")
    fun testValidateValidToken() {
        // Given
        val userId = 1L
        val role = "ROLE_USER"
        val expiredMs = 60000L
        val token = jwtUtil.createJwt(userId, role, expiredMs, "access")

        // When
        val isValid = jwtUtil.validateToken(token)

        // Then
        Assertions.assertThat(isValid).isTrue()
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰 검증 테스트")
    fun testValidateInvalidToken() {
        val invalidToken = "invalid.jwt.token"
        val isValid = jwtUtil.validateToken(invalidToken)
        Assertions.assertThat(isValid).isFalse()
    }

    @Test
    @DisplayName("만료된 JWT 토큰 검증 테스트")
    fun testValidateExpiredToken() {
        val userId = 1L
        val role = "ROLE_USER"
        val expiredMs = 0L
        val token = jwtUtil.createJwt(userId, role, expiredMs, "access")

        val isValid = jwtUtil.validateToken(token)

        Assertions.assertThat(isValid).isFalse()
    }
}