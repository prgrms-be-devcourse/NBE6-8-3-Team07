package com.back.fairytale.domain.thoughts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ThoughtsRequest(
        @NotNull(message = "동화 ID는 필수입니다.")
        Long fairytaleId,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 최대 50자까지 입력할 수 있습니다.")
        String name,

        @NotBlank(message = "아이생각은 필수입니다.")
        String content,

        @NotBlank(message = "부모생각은 필수입니다.")
        String parentContent
) {}
