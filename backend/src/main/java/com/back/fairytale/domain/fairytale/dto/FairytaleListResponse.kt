package com.back.fairytale.domain.fairytale.dto

import com.back.fairytale.domain.fairytale.entity.Fairytale
import java.time.LocalDate

data class FairytaleListResponse(
    val id: Long,
    val title: String,
    val imageUrl: String?,     // ← 이거만 nullable
    val isPublic: Boolean,
    val createdAt: LocalDate
) {
    constructor(fairytale: Fairytale) : this(
        id = fairytale.id!!,                       // non-null 보장
        title = fairytale.title,
        imageUrl = fairytale.imageUrl,             // nullable 허용
        isPublic = fairytale.isPublic,
        createdAt = fairytale.createdAt!!.toLocalDate() // non-null 보장
    )
}