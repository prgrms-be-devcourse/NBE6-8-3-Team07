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
            val id = keyword.keywordId ?: throw IllegalStateException("Keyword ID must not be null when mapping to response")
            return KeywordResponseDto(
                keywordId = id,
                keyword = keyword.keyword,
                keywordType = keyword.keywordType.name,
                usageCount = keyword.usageCount
            )
        }
    }
}
