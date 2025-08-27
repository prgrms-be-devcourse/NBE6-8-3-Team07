package com.back.fairytale.external.ai.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// Gemini에게 보낼 메시지를 JSON 형식으로 만들기 위한 구조
@Getter
@Builder
public class GeminiRequest {
    private List<Content> contents;

    @Getter
    @Builder
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    @Builder
    public static class Part {
        private String text;
    }

    public static GeminiRequest of(String prompt) {
        return GeminiRequest.builder()
                .contents(List.of(
                        Content.builder()
                            .parts(List.of(
                                Part.builder()
                                    .text(prompt)
                                    .build()
                            ))
                        .build()
                ))
                .build();
    }
}
