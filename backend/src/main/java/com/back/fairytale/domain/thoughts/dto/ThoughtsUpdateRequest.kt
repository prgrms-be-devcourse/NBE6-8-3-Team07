package com.back.fairytale.domain.thoughts.dto

import jakarta.validation.constraints.Size

data class ThoughtsUpdateRequest(
    @field:Size(max = 50, message = "이름은 최대 50자까지 입력할 수 있습니다.") val name: String?,

    val content: String?,

    val parentContent: String?
)
