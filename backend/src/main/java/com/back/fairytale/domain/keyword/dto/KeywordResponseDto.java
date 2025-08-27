package com.back.fairytale.domain.keyword.dto;

import com.back.fairytale.domain.keyword.entity.Keyword;

public record KeywordResponseDto(
        Long keywordId,
        String keyword,
        String keywordType,
        int usageCount
) {
    public static KeywordResponseDto fromEntity(Keyword keyword) {
        return new KeywordResponseDto(
                keyword.getKeywordId(),
                keyword.getKeyword(),
                keyword.getKeywordType() != null ? keyword.getKeywordType().name() : null,
                keyword.getUsageCount()
        );
    }
}