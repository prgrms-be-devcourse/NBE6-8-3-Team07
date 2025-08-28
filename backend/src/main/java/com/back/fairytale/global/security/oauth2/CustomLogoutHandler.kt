package com.back.fairytale.global.security.oauth2

import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.port.LogoutService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class CustomLogoutHandler(
    private val logoutService: LogoutService,
    private val jwtProvider: JWTProvider
) : LogoutHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(CustomLogoutHandler::class.java)
    }

    override fun logout(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
        if (request.method == "GET") {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "GET method not allowed")
            return
        }

        runCatching {
            val accessToken = jwtProvider.extractTokenFromCookies(
                request.cookies,
                JWTProvider.TokenType.ACCESS
            )

            if (accessToken != null && jwtProvider.validateAccessToken(accessToken)) {
                val userId = jwtProvider.getUserIdFromAccessToken(accessToken)
                logoutService.logout(userId)
            }
        }.onFailure { e ->
            logger.warn("Logout failed: {}", e.message)
        }

        response.apply {
            addCookie(jwtProvider.createCookie(null, "Authorization", 0))
            addCookie(jwtProvider.createCookie(null, "refresh", 0))
        }
    }
}
