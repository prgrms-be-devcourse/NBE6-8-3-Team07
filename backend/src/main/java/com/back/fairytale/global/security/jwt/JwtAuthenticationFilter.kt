package com.back.fairytale.global.security.jwt

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
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit


@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JWTUtil,
    private val jwtProvider: JWTProvider,
    private val userRepository: UserRepository,
    private val corsProperties: CorsProperties
) : OncePerRequestFilter() {

    companion object {
        private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
        private val SAFE_METHODS = setOf("GET", "OPTIONS")
        private const val TOKEN_GRACE_PERIOD_SECONDS = 10L
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
        if (!SAFE_METHODS.contains(request.method) && !isValidRequest(request)) {
            log.warn("CSRF 검증 실패: method={}, origin={}",
                request.method, request.getHeader("Origin"))
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid request")
            return
        }

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

        when {
            refreshToken == user.refreshToken -> {
                log.info("정상적인 토큰 재발급. 사용자 ID: {}", userId)

                val oldRefreshToken = user.refreshToken
                val newAccessToken = jwtProvider.createAccessToken(user.id!!, user.role.key)
                val newRefreshToken = jwtProvider.createRefreshToken(user.id!!, user.role.key)

                user.refreshToken = newRefreshToken
                user.previousRefreshToken = oldRefreshToken
                user.refreshTokenUpdatedAt = LocalDateTime.now()
                userRepository.save(user)

                response.apply {
                    addCookie(jwtProvider.wrapAccessTokenToCookie(newAccessToken))
                    addCookie(jwtProvider.wrapRefreshTokenToCookie(newRefreshToken))
                }

                log.info("토큰 재발급 완료. 사용자 ID: {}", userId)
                saveAuthenticate(user, request, response, filterChain)
            }

            isWithinGracePeriod(refreshToken, user) -> {
                log.info("유예 기간 내 이전 토큰 사용. 액세스 토큰만 재발급. 사용자 ID: {}", userId)
                issueAccessToken(user, response)
                saveAuthenticate(user, request, response, filterChain)
            }

            else -> {
                log.warn("유효하지 않은 리프레시 토큰. 사용자 ID: {}", userId)
                response.status = HttpServletResponse.SC_UNAUTHORIZED
            }
        }
    }

    private fun isWithinGracePeriod(requestedToken: String, user: User): Boolean {
        val previousToken = user.previousRefreshToken ?: return false
        val updatedAt = user.refreshTokenUpdatedAt ?: return false

        return requestedToken == previousToken &&
                ChronoUnit.SECONDS.between(updatedAt, LocalDateTime.now()) <= TOKEN_GRACE_PERIOD_SECONDS
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

    private fun issueAccessToken(user: User, response: HttpServletResponse) {
        val newAccessToken = jwtProvider.createAccessToken(user.id!!, user.role.key)
        response.addCookie(jwtProvider.wrapAccessTokenToCookie(newAccessToken))
        response.addCookie(jwtProvider.wrapRefreshTokenToCookie(user.refreshToken!!))
    }

    private fun isValidToken(token: String?): Boolean {
        return token != null && jwtUtil.validateToken(token)
    }

    private fun getTokenFromCookies(request: HttpServletRequest, name: String): String? {
        return request.cookies?.find { it.name == name }?.value
    }
}