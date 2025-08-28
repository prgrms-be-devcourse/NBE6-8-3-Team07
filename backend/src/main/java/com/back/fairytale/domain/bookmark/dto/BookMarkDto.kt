package com.back.fairytale.domain.bookmark.dto

import jakarta.validation.constraints.NotNull

data class BookMarkDto(
    @field:NotNull
    val fairytaleId: Long
)
