package com.back.fairytale.global.security.jwt

import com.back.fairytale.domain.refreshtoken.entity.RefreshToken
import com.back.fairytale.domain.refreshtoken.repository.RefreshTokenRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.global.security.CorsProperties
import com.back.fairytale.global.security.CustomOAuth2User
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JWTUtil,
    private val jwtProvider: JWTProvider,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val corsProperties: CorsProperties
) : OncePerRequestFilter() {

    companion object {
        private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
        private val SAFE_METHODS = setOf("GET", "OPTIONS")
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI.startsWith("/h2-console") ||
                request.requestURI.startsWith("/swagger") ||
                request.requestURI.startsWith("/favicon.ico")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // CSRF 체크
        if (!SAFE_METHODS.contains(request.method) && !isValidRequest(request)) {
            log.warn("CSRF 검증 실패: method={}, origin={}", request.method, request.getHeader("Origin"))
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid request")
            return
        }

        // Access Token 체크
        val accessToken = getTokenFromCookies(request, JWTProvider.TokenType.ACCESS.tokenName)
        if (isValidToken(accessToken)) {
            val userId = jwtUtil.getUserId(accessToken!!)
            val user = userRepository.findById(userId).orElse(null)
            if (user != null) {
                saveAuthenticate(user, request, response, filterChain)
                return
            }
        }

        // Refresh Token 체크
        val refreshTokenValue = getTokenFromCookies(request, JWTProvider.TokenType.REFRESH.tokenName)
        if (refreshTokenValue == null || !isValidToken(refreshTokenValue)) {
            filterChain.doFilter(request, response)
            return
        }

        reissueAccessToken(refreshTokenValue, request, response, filterChain)
    }

    private fun reissueAccessToken(
        refreshTokenValue: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val tokenEntity = refreshTokenRepository.findByTokenWithUser(refreshTokenValue)
        if (tokenEntity == null || jwtUtil.isExpired(tokenEntity.token)) {
            log.warn("유효하지 않은 refresh token")
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            return
        }

        val user = tokenEntity.user

        // 새 토큰 발급
        val newAccessToken = jwtProvider.createAccessToken(user.id!!, user.role.key)
        val newRefreshToken = jwtProvider.createRefreshToken(user.id!!, user.role.key)

        refreshTokenRepository.delete(tokenEntity)

        // 새 RefreshToken 저장
        val newRefreshEntity = RefreshToken(
            token = newRefreshToken,
            user = user,
        )
        refreshTokenRepository.save(newRefreshEntity)

        response.addCookie(jwtProvider.wrapAccessTokenToCookie(newAccessToken))
        response.addCookie(jwtProvider.wrapRefreshTokenToCookie(newRefreshToken))

        saveAuthenticate(user, request, response, filterChain)
    }

    private fun saveAuthenticate(
        user: User,
        request: HttpServletRequest,
        response: HttpServletResponse?,
        filterChain: FilterChain
    ) {
        val principal = CustomOAuth2User(user.id!!, user.socialId, user.role.key)
        val auth: Authentication = OAuth2AuthenticationToken(principal, principal.authorities, "naver")
        SecurityContextHolder.getContext().authentication = auth
        filterChain.doFilter(request, response)
    }

    private fun isValidRequest(request: HttpServletRequest): Boolean {
        val origin = request.getHeader("Origin")
        if (origin != null) {
            return corsProperties.allowedOrigins.contains(origin)
        }

        val referer = request.getHeader("Referer")
        if (referer != null && isRefererAllowed(referer)) {
            return true
        }

        return request.contentType.contains(MediaType.APPLICATION_JSON_VALUE)
    }

    private fun isRefererAllowed(referer: String): Boolean {
        return corsProperties.allowedOrigins.any { allowedOrigin ->
            referer.startsWith("$allowedOrigin/") || referer == allowedOrigin
        }
    }

    private fun isValidToken(token: String?): Boolean {
        return token != null && jwtUtil.validateToken(token)
    }

    private fun getTokenFromCookies(request: HttpServletRequest, name: String): String? {
        return request.cookies?.find { it.name == name }?.value
    }
}
