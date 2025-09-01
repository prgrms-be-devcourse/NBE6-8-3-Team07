package com.back.fairytale.global.security;

import com.back.fairytale.global.security.jwt.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JWTUtilTest {

    private JWTUtil jwtUtil;
    private final String testSecret = "testSecretKeyForJWTTokenGenerationAndValidation1234567890";

    @BeforeEach
    void setUp() {
        jwtUtil = new JWTUtil(testSecret);
    }

    @Test
    @DisplayName("JWT 토큰 생성 테스트")
    void testCreateJwt() {
        Long userId = 1L;
        String role = "ROLE_USER";
        Long expiredMs = 60000L;

        String token = jwtUtil.createJwt(userId, role, expiredMs, "access");

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT header.payload.signature 형태
    }

    @Test
    @DisplayName("유효한 JWT 토큰 검증 테스트")
    void testValidateValidToken() {
        // Given
        Long userId = 1L;
        String role = "ROLE_USER";
        Long expiredMs = 60000L;
        String token = jwtUtil.createJwt(userId, role, expiredMs, "access");

        // When
        boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰 검증 테스트")
    void testValidateInvalidToken() {
        String invalidToken = "invalid.jwt.token";
        boolean isValid = jwtUtil.validateToken(invalidToken);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료된 JWT 토큰 검증 테스트")
    void testValidateExpiredToken() {
        Long userId = 1L;
        String role = "ROLE_USER";
        Long expiredMs = 0L;
        String token = jwtUtil.createJwt(userId, role, expiredMs, "access");

        boolean isValid = jwtUtil.validateToken(token);

        assertThat(isValid).isFalse();
    }
}