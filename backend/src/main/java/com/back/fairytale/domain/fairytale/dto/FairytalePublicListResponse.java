package com.back.fairytale.domain.fairytale.dto;

import com.back.fairytale.domain.fairytale.entity.Fairytale;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

public record FairytalePublicListResponse(
        Long id,
        String title,
        String childName,
        String childRole,
        String characters,
        String place,
        String mood,
        String lesson,
        LocalDate createdAt,
        Long likeCount

) {
    public static FairytalePublicListResponse from(Fairytale fairytale) {
        return new FairytalePublicListResponse(
                fairytale.getId(),
                fairytale.getTitle(),
                fairytale.getChildName(),
                fairytale.getChildRole(),
                removeDuplicates(fairytale.getCharacters()),
                removeDuplicates(fairytale.getPlace()),
                removeDuplicates(fairytale.getMood()),
                removeDuplicates(fairytale.getLesson()),
                fairytale.getCreatedAt().toLocalDate(),
                fairytale.getLikeCount()
        );
    }

    // 키워드 중복 제거 메서드
    private static String removeDuplicates(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return keywords;
        }

        return Arrays.stream(keywords.split(", "))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }
}