package com.back.fairytale.domain.fairytale.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class FairytaleCreateRequest(
    @field:NotBlank(message = "아이 이름은 필수입니다")
    @field:Pattern(regexp = """^[가-힣a-zA-Z0-9\s]+$""", message = "부적절한 문자가 포함되어 있습니다.")
    @field:Size(min = 1, max = 50, message = "아이 이름은 1-50자 사이여야 합니다.")
    val childName: String,

    @field:NotBlank(message = "아이 역할은 필수입니다")
    @field:Pattern(regexp = """^[가-힣a-zA-Z0-9\s]+$""", message = "부적절한 문자가 포함되어 있습니다.")
    @field:Size(min = 1, max = 50, message = "아이 역할은 1-50자 사이여야 합니다.")
    val childRole: String,

    @field:NotBlank(message = "등장인물은 필수입니다")
    @field:Pattern(regexp = """^[가-힣a-zA-Z0-9\s,]+$""", message = "부적절한 문자가 포함되어 있습니다.")
    @field:Size(min = 1, max = 200, message = "등장인물은 1-200자 사이여야 합니다.")
    val characters: String,

    @field:NotBlank(message = "장소는 필수입니다")
    @field:Pattern(regexp = """^[가-힣a-zA-Z0-9\s,]+$""", message = "부적절한 문자가 포함되어 있습니다.")
    @field:Size(min = 1, max = 200, message = "장소는 1-200자 사이여야 합니다.")
    val place: String,

    @field:NotBlank(message = "교훈은 필수입니다")
    @field:Pattern(regexp = """^[가-힣a-zA-Z0-9\s,]+$""", message = "부적절한 문자가 포함되어 있습니다.")
    @field:Size(min = 1, max = 200, message = "교훈은 1-200자 사이여야 합니다.")
    val lesson: String,

    @field:NotBlank(message = "분위기는 필수입니다")
    @field:Pattern(regexp = """^[가-힣a-zA-Z0-9\s,]+$""", message = "부적절한 문자가 포함되어 있습니다.")
    @field:Size(min = 1, max = 200, message = "분위기는 1-200자 사이여야 합니다.")
    val mood: String
)