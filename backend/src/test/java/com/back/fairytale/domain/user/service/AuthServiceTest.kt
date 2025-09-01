package com.back.fairytale.domain.user.service

import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.enums.IsDeleted
import com.back.fairytale.domain.user.enums.Role
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import com.google.cloud.storage.Storage
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {
    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtProvider: JWTProvider

    private lateinit var testUser: User
    private lateinit var validRefreshToken: String

    @MockitoBean
    private lateinit var googleCloudStorage: GoogleCloudStorage

    @MockitoBean
    private lateinit var storage: Storage

    @BeforeEach
    fun setUp() {
        testUser = User(
            email = "test@example.com",
            name = "name",
            role = Role.USER,
            socialId = "testSocialId",
            nickname = "testNickname",
            isDeleted = IsDeleted.NOT_DELETED
        )

        val savedUser = userRepository.save(testUser)

        validRefreshToken = jwtProvider.createRefreshToken(savedUser.id!!, savedUser.role.key)
        savedUser.refreshToken = validRefreshToken
    }

    @Test
    @DisplayName("토큰 재발급이 성공적으로 이루어진다.")
    fun reissueToken() {
        // Given
        val refreshTokenCookie = Cookie("refresh", validRefreshToken)
        val cookies = arrayOf(refreshTokenCookie)

        // When
        val extractedRefreshToken = authService.getRefreshTokenFromCookies(cookies)
        val tokenPairDto = authService.reissueTokens(extractedRefreshToken)
        val newAccessToken = tokenPairDto.accessToken
        val newRefreshToken = tokenPairDto.refreshToken

        val accessTokenCookie = authService.createAccessTokenCookie(newAccessToken)
        val newRefreshTokenCookie = authService.createRefreshTokenCookie(newRefreshToken)

        // Then
        Assertions.assertThat(jwtProvider.validateAccessToken(newAccessToken)).isTrue()
        Assertions.assertThat(jwtProvider.validateRefreshToken(newRefreshToken)).isTrue()
        Assertions.assertThat(newRefreshToken).isNotEqualTo(validRefreshToken)
        Assertions.assertThat(accessTokenCookie.value).isEqualTo(newAccessToken)
        Assertions.assertThat(newRefreshTokenCookie.value).isEqualTo(newRefreshToken)

        val updatedUser = userRepository.findById(testUser.id!!).orElseThrow()
        Assertions.assertThat(updatedUser.refreshToken).isEqualTo(newRefreshToken)
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 재발급이 실패한다.")
    fun reissueToken_InvalidToken() {
        // Given
        val invalidRefreshToken = "invalid.refresh.token"

        // When & Then
        Assertions.assertThatThrownBy { authService.reissueTokens(invalidRefreshToken) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Refresh token이 유효하지 않습니다.")
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 재발급 시도 시 실패한다.")
    fun reissueToken_NonExistentUser() {
        // Given
        val nonExistentUserId = 999L
        val tokenForNonExistentUser = jwtProvider.createRefreshToken(nonExistentUserId, "USER")

        // When & Then
        Assertions.assertThatThrownBy {
            authService.reissueTokens(
                tokenForNonExistentUser
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("해당 유저가 존재하지 않습니다")
    }

    @Test
    @DisplayName("리프레시 토큰이 다를 경우 재발급이 실패한다.")
    fun reissueAccessToken_TokenMismatch() {
        // Given
        authService.reissueTokens(validRefreshToken)

        // When & Then - 원래 토큰으로 재발급 시도
        Assertions.assertThatThrownBy { authService.reissueTokens(validRefreshToken) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Refresh Token이 일치하지 않습니다.")
    }

    @Test
    @DisplayName("로그아웃이 성공적으로 이루어진다.")
    fun logout() {
        // When
        authService.logout(testUser.id!!)

        // Then
        val logOutUser = userRepository.findById(testUser.id!!).orElseThrow()
        Assertions.assertThat(logOutUser.refreshToken).isNull()
    }

    @Test
    @DisplayName("존재하지 않는 사용자 로그아웃 시도 시 실패한다.")
    fun logout_NonExistentUser() {
        // Given
        val nonExistentUserId = 999L

        // When & Then
        Assertions.assertThatThrownBy { authService.logout(nonExistentUserId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("해당 유저가 존재하지 않습니다")
    }
}