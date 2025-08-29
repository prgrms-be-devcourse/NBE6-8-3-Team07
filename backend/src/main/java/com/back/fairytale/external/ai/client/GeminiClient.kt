package com.back.fairytale.external.ai.client

import com.back.fairytale.external.ai.dto.GeminiRequest
import com.back.fairytale.external.ai.dto.GeminiResponse
import com.back.fairytale.external.exception.GeminiApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import reactor.util.retry.Retry
import java.time.Duration

@Component
class GeminiClient(
    private val webClient: WebClient,
    @Value("\${gemini.api.url}") private val apiUrl: String,
    @Value("\${gemini.api.key}") private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateFairytale(prompt: String): String {
        return try {
            val request = GeminiRequest.of(prompt)
            val urlWithKey = buildApiUrl()

            log.info("Gemini API 호출: {}", prompt)

            val response = webClient.post()
                .uri(urlWithKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse::class.java)
                .retryWhen(
                    Retry.backoff(2, Duration.ofSeconds(2))
                        .filter { it is WebClientResponseException && (it.statusCode.is5xxServerError) }
                )
                .timeout(Duration.ofSeconds(30))
                .block()

            val generated = response?.getGeneratedText().orEmpty()
            if (generated.isBlank()) throw GeminiApiException("Gemini API에서 빈 응답을 받았습니다.")
            generated
        } catch (e: WebClientResponseException) {
            log.error("Gemini API 호출 오류 - 상태코드: {}, 응답: {}", e.statusCode, e.responseBodyAsString, e)
            throw GeminiApiException("Gemini API 호출 실패: ${e.statusCode} / ${e.responseBodyAsString}")
        } catch (e: Exception) {
            log.error("Gemini API 호출 중 오류 발생", e)
            throw GeminiApiException("Gemini API 연결 실패: ${e.message}")
        }
    }

    private fun buildApiUrl(): String =
        UriComponentsBuilder.fromUriString(apiUrl)
            .queryParam("key", apiKey)
            .toUriString()
}