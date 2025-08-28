package com.back.fairytale.domain.like.dto

import jakarta.validation.constraints.NotNull

data class LikeDto(
    @field:NotNull
    val fairytaleId: Long
)