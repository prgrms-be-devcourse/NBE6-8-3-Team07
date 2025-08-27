package com.back.fairytale.domain.fairytale.dto;

import com.back.fairytale.domain.fairytale.entity.Fairytale;

import java.time.LocalDateTime;

public record FairytaleResponse (
    Long id,
    String title,
    String content,
    String imageUrl,
    String childName,
    String childRole,
    String characters,
    String place,
    String lesson,
    String mood,
    Long userId,
    LocalDateTime createdAt
) {
    public static FairytaleResponse from(Fairytale fairytale) {
        return new FairytaleResponse(
                fairytale.getId(),
                fairytale.getTitle(),
                fairytale.getContent(),
                fairytale.getImageUrl(),
                fairytale.getChildName(),
                fairytale.getChildRole(),
                fairytale.getCharacters(),
                fairytale.getPlace(),
                fairytale.getLesson(),
                fairytale.getMood(),
                fairytale.getUser().getId(),
                fairytale.getCreatedAt()
        );
    }
}
