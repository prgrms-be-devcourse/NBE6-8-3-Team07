package com.back.fairytale.external.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// Gemini API로부터 받은 응답 JSON을 Java 객체로 파싱하기 위한 구조
@Getter
@NoArgsConstructor
public class GeminiResponse {
    private List<Candidate> candidates;

    @Getter
    @NoArgsConstructor
    public static class Candidate {
        private Content content;
    }

    @Getter
    @NoArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    @NoArgsConstructor
    public static class Part {
        private String text;
    }

    public String getGeneratedText() {
        if (candidates != null && !candidates.isEmpty() &&
                candidates.get(0).content != null &&
                candidates.get(0).content.parts != null &&
                !candidates.get(0).content.parts.isEmpty()) {
            return candidates.get(0).content.parts.get(0).text;
        }
        return "";
    }
}
