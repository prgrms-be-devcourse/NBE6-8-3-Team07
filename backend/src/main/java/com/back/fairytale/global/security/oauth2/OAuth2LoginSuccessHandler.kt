package com.back.fairytale.global.security.oauth2

import com.back.fairytale.global.security.CorsProperties
import com.back.fairytale.global.security.CustomOAuth2User
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.port.UserTokenService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@RequiredArgsConstructor
@Component
@Slf4j
class OAuth2LoginSuccessHandler(
    private val jwtProvider: JWTProvider,
    private val userTokenService: UserTokenService,
    private val corsProperties: CorsProperties
) : AuthenticationSuccessHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler::class.java)
    }

    override fun onAuthenticationSuccess(
        request: HttpServletRequest?,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        logger.info("OAuth2 로그인 성공: {}", authentication.name)
        val customUser = authentication.principal as CustomOAuth2User
        val userId = customUser.id

        val refreshToken = jwtProvider.createRefreshToken(userId, customUser.role)
        userTokenService.saveOrUpdateUserToken(userId, refreshToken)

        val refreshCookie = jwtProvider.createRefreshTokenCookie(refreshToken)

        val redirectUrl: String = corsProperties.allowedOrigins.first()

        response.apply {
            addCookie(refreshCookie)
            sendRedirect(redirectUrl)
        }
    }
}
