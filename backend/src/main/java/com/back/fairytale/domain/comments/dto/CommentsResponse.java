package com.back.fairytale.domain.comments.dto;

import com.back.fairytale.domain.comments.entity.Comments;

import java.time.LocalDateTime;

public record CommentsResponse (
    Long id,
    Long fairytaleId,
    String nickname,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CommentsResponse from(Comments comments) {
        return new CommentsResponse(
            comments.getId(),
            comments.getFairytale().getId(),
            comments.getUser().getNickname(),
            comments.getContent(),
            comments.getCreatedAt(),
            comments.getUpdatedAt()
        );
    }
}
