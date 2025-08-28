package com.back.fairytale.domain.comments.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CommentsUpdateRequest(
    @field:NotBlank(message = "댓글 내용은 비워둘 수 없습니다.")
    @field:Size(
        max = 500,
        message = "댓글은 최대 500자까지 입력할 수 있습니다."
    )
    val content: String
)
