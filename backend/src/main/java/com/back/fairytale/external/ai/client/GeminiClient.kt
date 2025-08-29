package com.back.fairytale.external.ai.client

import com.back.fairytale.external.ai.dto.GeminiRequest
import com.back.fairytale.external.ai.dto.GeminiResponse
import com.back.fairytale.external.exception.GeminiApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class GeminiClient(
    private val restTemplate: RestTemplate,
    @Value("\${gemini.api.url}") private val apiUrl: String,
    @Value("\${gemini.api.key}") private val apiKey: String
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun generateFairytale(prompt: String): String {
        try {
            val request = GeminiRequest.of(prompt)

            // 요청 헤더 설정
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val urlWithKey = buildApiUrl()

            val entity = HttpEntity(request, headers) // 요청 데이터 + 헤더

            log.info("Gemini API 호출: {}", prompt)

            // API 호출
            val response = restTemplate.postForEntity(
                urlWithKey,
                entity,
                GeminiResponse::class.java
            )

            if (response.statusCode == HttpStatus.OK && response.body != null) {
                val generatedText = response.body!!.getGeneratedText()
                if (generatedText.isNullOrBlank()) {
                    throw GeminiApiException("Gemini API에서 빈 응답을 받았습니다.")
                }
                return generatedText
            } else {
                throw GeminiApiException("Gemini API 호출 실패: ${response.statusCode}")
            }
        } catch (e: Exception) {
            log.error("Gemini API 호출 중 오류 발생", e)
            throw GeminiApiException("Gemini API 연결 실패: ${e.message}")
        }
    }

    // UriComponentsBuilder로 URL 생성
    private fun buildApiUrl(): String =
        UriComponentsBuilder.fromHttpUrl(apiUrl)
            .queryParam("key", apiKey)
            .toUriString()
}