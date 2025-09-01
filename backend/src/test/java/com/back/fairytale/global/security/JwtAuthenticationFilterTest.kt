package com.back.fairytale.global.security

import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.enums.Role
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.jwt.JWTUtil
import com.back.fairytale.global.security.jwt.JwtAuthenticationFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

class JwtAuthenticationFilterTest {

    private val jwtUtil: JWTUtil = mock(JWTUtil::class.java)
    private val jwtProvider: JWTProvider = mock(JWTProvider::class.java)
    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val filter = JwtAuthenticationFilter(jwtUtil, jwtProvider, userRepository)

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
        request.setCookies(Cookie(JWTProvider.TokenType.ACCESS.tokenName, accessToken))

        val user = User(
            id = 1L,
            refreshToken = "refresh123",
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
    @DisplayName("유효하지 않은 토큰이 넘어오면 인증이 설정되지 않는다.")
    fun fail_invalidTokens() {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        val invalidAccess = "invalid-access-token"
        val invalidRefresh = "invalid-refresh-token"
        request.setCookies(
            Cookie(JWTProvider.TokenType.ACCESS.tokenName, invalidAccess),
            Cookie(JWTProvider.TokenType.REFRESH.tokenName, invalidRefresh)
        )

        given(jwtUtil.validateToken(invalidAccess)).willReturn(false)
        given(jwtUtil.validateToken(invalidRefresh)).willReturn(false)

        // when
        filter.doFilter(request, response, chain)

        // then
        val auth = SecurityContextHolder.getContext().authentication
        assert(auth == null)
    }
}

