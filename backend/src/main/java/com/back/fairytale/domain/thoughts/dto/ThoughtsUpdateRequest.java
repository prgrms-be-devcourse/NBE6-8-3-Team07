package com.back.fairytale.domain.thoughts.dto;

import jakarta.validation.constraints.Size;

public record ThoughtsUpdateRequest(

        @Size(max = 50, message = "이름은 최대 50자까지 입력할 수 있습니다.")
        String name,

        String content,

        String parentContent
) {}
