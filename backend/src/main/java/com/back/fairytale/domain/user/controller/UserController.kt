package com.back.fairytale.domain.user.controller

import com.back.fairytale.domain.user.service.AuthService
import com.back.fairytale.global.security.CustomOAuth2User
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val authService: AuthService
) {

    @PostMapping("/reissue")
    fun reissue(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Any> {
        return runCatching {
            val refreshToken = authService.getRefreshTokenFromCookies(request.cookies)

            val tokenPairDto = authService.reissueTokens(refreshToken)

            response.apply {
                addCookie(authService.createAccessTokenCookie(tokenPairDto.accessToken))
                addCookie(authService.createRefreshTokenCookie(tokenPairDto.refreshToken))
            }
            ResponseEntity.ok().build<Any>()
        }.getOrElse { e ->
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @GetMapping("/users/me")
    fun getCurrentUser(
        @AuthenticationPrincipal oAuth2User: CustomOAuth2User?
    ): ResponseEntity<Any> =
        oAuth2User?.let {
            ResponseEntity.ok(null)
        } ?: ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("message" to "인증이 필요한 서비스입니다."))
}