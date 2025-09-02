package com.back.fairytale.domain.user.service

import com.back.fairytale.domain.refreshtoken.entity.RefreshToken
import com.back.fairytale.domain.refreshtoken.repository.RefreshTokenRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.enums.IsDeleted
import com.back.fairytale.domain.user.enums.Role
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.jwt.JWTUtil
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
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var jwtProvider: JWTProvider

    @Autowired
    private lateinit var jWTUtil: JWTUtil

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

        val refreshTokenEntity = RefreshToken(
            token = validRefreshToken,
            user = savedUser
        )
        refreshTokenRepository.save(refreshTokenEntity)
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

        val updatedRefreshToken = refreshTokenRepository.findByToken(newRefreshToken)
        Assertions.assertThat(updatedRefreshToken).isNotNull()
        Assertions.assertThat(updatedRefreshToken!!.user.id).isEqualTo(testUser.id)
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
    @DisplayName("만료된 리프레시 토큰으로 재발급이 실패한다.")
    fun reissueAccessToken_ExpiredToken() {
        // Given - 만료된 토큰 생성 (JWT 생성 시 과거 시간으로 설정)
        val expiredToken = jWTUtil.createJwt(testUser.id!!, testUser.role.key, -1000L, "refresh")

        // When & Then
        Assertions.assertThatThrownBy { authService.reissueTokens(expiredToken) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Refresh token이 유효하지 않습니다.")
    }

    @Test
    @DisplayName("로그아웃이 성공적으로 이루어진다.")
    fun logout() {
        // When
        authService.logout(testUser.id!!)

        // Then
        val userTokens = refreshTokenRepository.findAllByUserId(testUser.id!!)
        Assertions.assertThat(userTokens).isEmpty()
    }
}