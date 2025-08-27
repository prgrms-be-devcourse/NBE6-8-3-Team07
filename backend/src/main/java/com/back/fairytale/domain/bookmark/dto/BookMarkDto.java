package com.back.fairytale.domain.bookmark.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookMarkDto {

    @NotNull
    private Long fairytaleId;

}
