package com.back.fairytale.global.security

import com.back.fairytale.domain.refreshtoken.entity.RefreshToken
import com.back.fairytale.domain.refreshtoken.repository.RefreshTokenRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.enums.Role
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.jwt.JWTProvider.TokenType
import com.back.fairytale.global.security.jwt.JWTUtil
import com.back.fairytale.global.security.jwt.JwtAuthenticationFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.antlr.v4.runtime.Token
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*
import kotlin.test.assertEquals

class JwtAuthenticationFilterTest {

    private val jwtUtil: JWTUtil = mock(JWTUtil::class.java)
    private val jwtProvider: JWTProvider = mock(JWTProvider::class.java)
    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val refreshTokenRepository: RefreshTokenRepository = mock(RefreshTokenRepository::class.java)
    private val corsProperties: CorsProperties = CorsProperties(
        allowedOrigins = listOf("http://localhost:3000")
    )
    private val filter = JwtAuthenticationFilter(jwtUtil, jwtProvider, userRepository, refreshTokenRepository, corsProperties)

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("유효한 액세스 토큰이 넘어오면 필터가 성공적으로 인증을 처리한다.")
    fun success_validAccessToken() {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        val accessToken = "valid-access-token"
        request.apply {
            method = "POST"
            addHeader("Origin", corsProperties.allowedOrigins.first())
            setCookies(Cookie(TokenType.ACCESS.tokenName, accessToken))
        }

        val user = User(
            id = 1L,
            name = "TestUser",
            nickname = "nick",
            email = "test@test.com",
            socialId = "social123",
            role = Role.USER
        )

        given(jwtUtil.validateToken(accessToken)).willReturn(true)
        given(jwtUtil.getUserId(accessToken)).willReturn(1L)
        given(userRepository.findById(1L)).willReturn(Optional.of(user))

        // when
        filter.doFilter(request, response, chain)

        // then
        val auth = SecurityContextHolder.getContext().authentication
        assert(auth != null && auth.isAuthenticated)
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰이 넘어오면 인증이 설정되지 않는다.")
    fun fail_invalidTokens() {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        val invalidAccess = "invalid-access-token"
        val invalidRefresh = "invalid-refresh-token"

        request.apply {
            method = "POST"
            addHeader("Origin", corsProperties.allowedOrigins.first())
            setCookies(
                Cookie(TokenType.ACCESS.tokenName, invalidAccess),
                Cookie(TokenType.REFRESH.tokenName, invalidRefresh)
            )
        }

        given(jwtUtil.validateToken(invalidAccess)).willReturn(false)
        given(jwtUtil.validateToken(invalidRefresh)).willReturn(false)

        // when
        filter.doFilter(request, response, chain)

        // then
        val auth = SecurityContextHolder.getContext().authentication
        assert(auth == null)
    }

    @Test
    @DisplayName
        ("리프레시 토큰이 일치하면 새로운 액세스 토큰과 리프레시 토큰을 발급한다.")
    fun success_refreshTokenMatchIssueNewTokens() {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)
        val refreshToken = "current-refresh-token"

        request.apply {
            method = "POST"
            addHeader("Origin", corsProperties.allowedOrigins.first())
            setCookies(
                Cookie(TokenType.REFRESH.tokenName, refreshToken)
            )
        }

        val user = User(
            id = 1L,
            name = "TestUser",
            nickname = "nick",
            email = "test@test.com",
            socialId = "social123",
            role = Role.USER
        )

        val refreshTokenEntity = RefreshToken(
            id = 1L,
            token = refreshToken,
            user = user
        )

        val newAccessToken = "new-access-token"
        val newRefreshToken = "new-refresh-token"

        given(refreshTokenRepository.findByTokenWithUser(refreshToken)).willReturn(refreshTokenEntity)
        given(jwtUtil.isExpired(refreshToken)).willReturn(false)
        given(jwtProvider.createAccessToken(1L, Role.USER.key)).willReturn(newAccessToken)
        given(jwtProvider.createRefreshToken(1L, Role.USER.key)).willReturn(newRefreshToken)
        given(jwtUtil.validateToken(refreshToken)).willReturn(true)
        given(jwtProvider.wrapAccessTokenToCookie(newAccessToken))
            .willReturn(Cookie(TokenType.ACCESS.tokenName, newAccessToken))
        given(jwtProvider.wrapRefreshTokenToCookie(newRefreshToken))
            .willReturn(Cookie(TokenType.REFRESH.tokenName, newRefreshToken))

        // when
        filter.doFilter(request, response, chain)

        // then
        val auth = SecurityContextHolder.getContext().authentication
        assert(auth != null && auth.isAuthenticated)

        verify(jwtProvider, times(1)).createAccessToken(1L, Role.USER.key)
        verify(jwtProvider, times(1)).createRefreshToken(1L, Role.USER.key)

        verify(refreshTokenRepository, times(1)).delete(refreshTokenEntity)
        verify(refreshTokenRepository, times(1)).save(argThat { token ->
            token.token == newRefreshToken && token.user.id == 1L
        })

        verify(chain, times(1)).doFilter(request, response)
    }


    @Test
    @DisplayName("리프레시 토큰으로 재발급 시도 중 사용자가 존재하지 않으면 401 상태를 반환한다.")
    fun fail_refreshTokenReissueUserNotFound() {
        // given
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)
        val refreshToken = "valid-refresh-token"
        val request = MockHttpServletRequest().apply {
            method = "POST"
            addHeader("Origin", corsProperties.allowedOrigins.first())
            setCookies(Cookie(TokenType.REFRESH.tokenName, refreshToken))
        }

        given(jwtUtil.validateToken(refreshToken)).willReturn(true)
        given(jwtUtil.getUserId(refreshToken)).willReturn(1L)
        given(userRepository.findById(1L)).willReturn(Optional.empty())

        // when
        filter.doFilter(request, response, chain)

        // then
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
        val auth = SecurityContextHolder.getContext().authentication
        assert(auth == null)
        verify(chain, never()).doFilter(any(), any())
    }

    @Test
    @DisplayName("GET 요청에 대해서는 CSRF 검증을 하지 않는다.")
    fun success_safeMethodSkipsCsrfValidation() {
        // given
        val request = MockHttpServletRequest().apply {
            method = "GET"
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        // when
        filter.doFilter(request, response, chain)

        // then
        verify(chain, times(1)).doFilter(request, response)
    }

    @Test
    @DisplayName("POST 요청에서 Origin이 허용되지 않으면 403을 반환한다.")
    fun fail_postRequestWithInvalidOrigin() {
        // given
        val request = MockHttpServletRequest().apply {
            method = "POST"
            addHeader("Origin", "https://example.com")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        // when
        filter.doFilter(request, response, chain)

        // then
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.status)
        verify(chain, never()).doFilter(any(), any())
    }

    @Test
    @DisplayName("POST 요청에서 Origin은 없지만 유효한 Referer가 있으면 통과한다.")
    fun success_postRequestWithValidReferer() {
        // given
        val request = MockHttpServletRequest().apply {
            method = "POST"
            addHeader("Referer", "${corsProperties.allowedOrigins.first()}/login")
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        // when
        filter.doFilter(request, response, chain)

        // then
        verify(chain, times(1)).doFilter(request, response)
    }
}

