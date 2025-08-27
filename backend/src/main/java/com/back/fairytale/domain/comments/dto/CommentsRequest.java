package com.back.fairytale.domain.comments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentsRequest (
        @NotNull(message = "동화 ID는 필수입니다.")
        Long fairytaleId,

        @NotBlank(message = "댓글 내용은 비워둘 수 없습니다.")
        @Size(max = 500, message = "댓글은 최대 500자까지 입력할 수 있습니다.")
        String content
) {}
