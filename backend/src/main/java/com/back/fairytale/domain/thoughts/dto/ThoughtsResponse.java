package com.back.fairytale.domain.thoughts.dto;

import com.back.fairytale.domain.thoughts.entity.Thoughts;

public record ThoughtsResponse (
    Long id,
    Long fairytaleId,
    String name,
    String content,
    String parentContent
) {
    public static ThoughtsResponse from(Thoughts thoughts) {
        return new ThoughtsResponse(
            thoughts.getId(),
            thoughts.getFairytale().getId(),
            thoughts.getName(),
            thoughts.getContent(),
            thoughts.getParentContent()
        );
    }
}
