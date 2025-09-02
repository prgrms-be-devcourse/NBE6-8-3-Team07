package com.back.fairytale.domain.fairytale.dto.search

data class FairytaleSearchRequest (
    val keyword: String,
    val page: Int,
    val size: Int,
    val sortBy: String, // 정렬 기준 (예: relevance, date, popularity 등)
    val scope: String, // 검색 범위 (예: title, author, content 등)
)