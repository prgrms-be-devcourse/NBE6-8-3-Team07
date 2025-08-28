package com.back.fairytale.global.security.jwt

import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.global.security.CustomOAuth2User
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JWTUtil,
    private val jwtProvider: JWTProvider,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    companion object {
        private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI.startsWith("/h2-console") || request.requestURI == "/reissue"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val accessToken = getTokenFromCookies(request, JWTProvider.TokenType.ACCESS.tokenName)

        if (isValidToken(accessToken)) {
            val userId = jwtUtil.getUserId(accessToken!!)
            val optionalUser = userRepository.findById(userId)

            if (optionalUser.isPresent) {
                saveAuthenticate(optionalUser.get(), request, response, filterChain)
                return
            }

            log.warn("유효한 토큰이나 사용자 없음. ID: {}", userId)
            filterChain.doFilter(request, response)
            return
        }

        val refreshToken = getTokenFromCookies(request, JWTProvider.TokenType.REFRESH.tokenName)
        if (!isValidToken(refreshToken)) {
            log.info("리프레시 토큰이 유효하지 않습니다.")
            filterChain.doFilter(request, response)
            return
        }

        reissueTokens(refreshToken!!, request, response, filterChain)
    }

    private fun reissueTokens(
        refreshToken: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val userId = jwtUtil.getUserId(refreshToken)
        val optionalUser = userRepository.findById(userId)

        if (optionalUser.isEmpty) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            return
        }

        val user = optionalUser.get()
        if (refreshToken != user.getRefreshToken()) {
            log.info("이미 갱신된 토큰 요청. 사용자 ID: {}", userId)
            issueAccessToken(user, response)
            saveAuthenticate(user, request, response, filterChain)
            return
        }

        val newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().getKey())
        val newRefreshToken = jwtProvider.createRefreshToken(user.getId(), user.getRole().getKey())

        user.setRefreshToken(newRefreshToken)
        userRepository.save(user)

        response.apply {
            addCookie(jwtProvider.wrapAccessTokenToCookie(newAccessToken))
            addCookie(jwtProvider.wrapRefreshTokenToCookie(newRefreshToken))
        }

        log.info("토큰 재발급 완료. 사용자 ID: {}", userId)
        saveAuthenticate(user, request, response, filterChain)
    }

    private fun saveAuthenticate(
        user: User,
        request: HttpServletRequest,
        response: HttpServletResponse?,
        filterChain: FilterChain
    ) {
        val principal = CustomOAuth2User(user.getId(), user.getSocialId(), user.getRole().getKey())
        val auth: Authentication = OAuth2AuthenticationToken(principal, principal.authorities, "naver")
        SecurityContextHolder.getContext().authentication = auth
        filterChain.doFilter(request, response)
    }

    private fun issueAccessToken(user: User, response: HttpServletResponse) {
        val newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().getKey())
        response.addCookie(jwtProvider.wrapAccessTokenToCookie(newAccessToken))
        response.addCookie(jwtProvider.wrapRefreshTokenToCookie(user.getRefreshToken()))
    }

    private fun isValidToken(token: String?): Boolean {
        return token != null && jwtUtil.validateToken(token)
    }

    private fun getTokenFromCookies(request: HttpServletRequest, name: String): String? {
        return request.cookies?.find { it.name == name }?.value
    }
}