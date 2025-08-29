package com.back.fairytale.external.ai.client

import com.back.fairytale.external.ai.dto.HuggingFaceImageRequest
import com.back.fairytale.external.exception.HuggingFaceApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import java.time.Duration

@Component
class HuggingFaceClient(
    private val webClient: WebClient,
    @Value("\${huggingface.api.url}") private val apiUrl: String,
    @Value("\${huggingface.api.token}") private val apiToken: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateImage(prompt: String): ByteArray {
        return try {
            log.info("HuggingFace 이미지 생성 요청: {}", prompt)

            val request = HuggingFaceImageRequest(prompt)

            val imageData = webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .retryWhen(
                    Retry.backoff(3, Duration.ofSeconds(20))
                        .filter { t ->
                            if (t is WebClientResponseException) {
                                val is503 = t.statusCode == HttpStatus.SERVICE_UNAVAILABLE
                                if (is503) log.warn("HuggingFace 모델 로딩 중... 재시도합니다.")
                                is503
                            } else {
                                false
                            }
                        }
                )
                .timeout(Duration.ofMinutes(2))
                .block()

            if (imageData == null || imageData.isEmpty()) {
                throw HuggingFaceApiException("빈 이미지 데이터를 받았습니다.")
            }

            log.info("이미지 생성 완료 - 크기: {} bytes", imageData.size)
            imageData
        } catch (e: WebClientResponseException) {
            log.error(
                "HuggingFace API 호출 오류 - 상태코드: {}, 응답: {}",
                e.statusCode, e.responseBodyAsString, e
            )
            when {
                e.statusCode == HttpStatus.SERVICE_UNAVAILABLE ->
                    throw HuggingFaceApiException("모델이 로딩 중입니다. 잠시 후 다시 시도해주세요.")
                e.statusCode.is4xxClientError ->
                    throw HuggingFaceApiException("클라이언트 오류: ${e.responseBodyAsString}")
                e.statusCode.is5xxServerError ->
                    throw HuggingFaceApiException("서버 오류: ${e.responseBodyAsString}")
                else ->
                    throw HuggingFaceApiException("이미지 생성 API 호출 실패: ${e.message}")
            }
        } catch (e: Exception) {
            log.error("HuggingFace 이미지 생성 중 오류 발생", e)
            throw HuggingFaceApiException("이미지 생성 실패: ${e.message}")
        }
    }
}