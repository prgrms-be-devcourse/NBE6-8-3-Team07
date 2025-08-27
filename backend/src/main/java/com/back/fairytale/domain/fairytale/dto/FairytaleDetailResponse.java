package com.back.fairytale.domain.fairytale.dto;

import com.back.fairytale.domain.fairytale.entity.Fairytale;

import java.time.LocalDateTime;

public record FairytaleDetailResponse (
    Long id,
    String title,
    String content,
    String imageUrl,
    boolean isPublic,
    String childName,
    String childRole,
    String characters,
    String place,
    String lesson,
    String mood,
    LocalDateTime createdAt
) {
    public static FairytaleDetailResponse from(Fairytale fairytale) {
        return new FairytaleDetailResponse(
                fairytale.getId(),
                fairytale.getTitle(),
                fairytale.getContent(),
                fairytale.getImageUrl(),
                fairytale.getIsPublic(),
                fairytale.getChildName(),
                fairytale.getChildRole(),
                fairytale.getCharacters(),
                fairytale.getPlace(),
                fairytale.getLesson(),
                fairytale.getMood(),
                fairytale.getCreatedAt()
        );
    }
}
