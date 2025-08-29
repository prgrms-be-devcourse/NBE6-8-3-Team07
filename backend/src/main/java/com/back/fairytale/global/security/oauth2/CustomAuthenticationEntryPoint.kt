package com.back.fairytale.global.security.oauth2

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest?, response: HttpServletResponse,
        authException: AuthenticationException?
    ) {
        response.apply {
            status = HttpStatus.UNAUTHORIZED.value()
            contentType = MediaType.APPLICATION_JSON_VALUE
            characterEncoding = "UTF-8"
        }
        val errorResponse = mapOf(
            "errorCode" to HttpStatus.UNAUTHORIZED.value(),
            "message" to "인증이 필요한 서비스입니다.",
            "timestamp" to Instant.now().toString()
        )

        objectMapper.writeValue(response.writer, errorResponse)
    }
}
