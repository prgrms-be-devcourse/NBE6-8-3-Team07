package com.back.fairytale.domain.keyword.dto

import com.back.fairytale.domain.keyword.entity.Keyword

data class KeywordResponseDto(
    val keywordId: Long,
    val keyword: String,
    val keywordType: String,
    val usageCount: Int
) {
    companion object {
        fun fromEntity(keyword: Keyword): KeywordResponseDto {
            return KeywordResponseDto(
                keywordId = keyword.keywordId ?: 0L,
                keyword = keyword.keyword,
                keywordType = keyword.keywordType?.name ?: "",
                usageCount = keyword.usageCount
            )
        }
    }
}
