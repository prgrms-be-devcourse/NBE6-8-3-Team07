package com.back.fairytale.domain.keyword.entity;

import com.back.fairytale.domain.keyword.enums.KeywordType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "keyword")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Long keywordId;

    @Column(nullable = false, length = 50)
    @Pattern(regexp = "^[가-힣a-zA-Z0-9\\s]+$", message = "부적절한 문자가 포함되어 있습니다.")
    @Size(min = 1, max = 50, message = "키워드는 1-50자 사이여야 합니다.")
    @NotBlank(message = "키워드는 필수입니다.")
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private KeywordType keywordType;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;
    //동시성 처리를 위한 버전 필드
    @Version
    private Long version;

    public static Keyword of(String keyword, KeywordType keywordType) {
        return Keyword.builder()
                .keyword(keyword.trim())
                .keywordType(keywordType)
                .usageCount(0)
                .build();
    }
    public void incrementUsageCount() {
        this.usageCount++;
    }
}