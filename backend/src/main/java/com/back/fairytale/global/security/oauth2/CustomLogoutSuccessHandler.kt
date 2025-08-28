package com.back.fairytale.global.security.oauth2

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class CustomLogoutSuccessHandler(
    private val objectMapper: ObjectMapper
) : LogoutSuccessHandler {
    @Throws(IOException::class)
    override fun onLogoutSuccess(
        request: HttpServletRequest?, response: HttpServletResponse,
        authentication: Authentication?
    ) {
        response.apply {
            status = HttpStatus.OK.value()
            contentType = MediaType.APPLICATION_JSON_VALUE
            characterEncoding = "UTF-8"
        }

        val successResponse = mapOf("message" to "Logout successful")
        objectMapper.writeValue(response.writer, successResponse)
    }
}
