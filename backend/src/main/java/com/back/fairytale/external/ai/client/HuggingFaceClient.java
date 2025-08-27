package com.back.fairytale.external.ai.client;

import com.back.fairytale.external.ai.dto.HuggingFaceImageRequest;
import com.back.fairytale.external.exception.HuggingFaceApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class HuggingFaceClient {

    private final WebClient webClient;

    @Value("${huggingface.api.url}")
    private String apiUrl;

    @Value("${huggingface.api.token}")
    private String apiToken;

    public byte[] generateImage(String prompt) {
        try {
            log.info("HuggingFace 이미지 생성 요청: {}", prompt);

            HuggingFaceImageRequest request = new HuggingFaceImageRequest(prompt);

            byte[] imageData = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(20))
                            .filter(throwable -> {
                                if (throwable instanceof WebClientResponseException) {
                                    WebClientResponseException webEx = (WebClientResponseException) throwable;
                                    boolean isServiceUnavailable = webEx.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE;
                                    if (isServiceUnavailable) {
                                        log.warn("HuggingFace 모델 로딩 중... 재시도합니다.");
                                    }
                                    return isServiceUnavailable;
                                }
                                return false;
                            }))
                    .timeout(Duration.ofMinutes(2))
                    .block();

            if (imageData == null || imageData.length == 0) {
                throw new HuggingFaceApiException("빈 이미지 데이터를 받았습니다.");
            }

            log.info("이미지 생성 완료 - 크기: {} bytes", imageData.length);
            return imageData;

        } catch (WebClientResponseException e) {
            log.error("HuggingFace API 호출 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);

            if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                throw new HuggingFaceApiException("모델이 로딩 중입니다. 잠시 후 다시 시도해주세요.");
            } else if (e.getStatusCode().is4xxClientError()) {
                throw new HuggingFaceApiException("클라이언트 오류: " + e.getResponseBodyAsString());
            } else if (e.getStatusCode().is5xxServerError()) {
                throw new HuggingFaceApiException("서버 오류: " + e.getResponseBodyAsString());
            }

            throw new HuggingFaceApiException("이미지 생성 API 호출 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("HuggingFace 이미지 생성 중 오류 발생", e);
            throw new HuggingFaceApiException("이미지 생성 실패: " + e.getMessage());
        }
    }
}
