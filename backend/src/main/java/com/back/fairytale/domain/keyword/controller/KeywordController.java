package com.back.fairytale.domain.keyword.controller;

import com.back.fairytale.domain.keyword.dto.KeywordResponseDto;
import com.back.fairytale.domain.keyword.enums.KeywordType;
import com.back.fairytale.domain.keyword.service.KeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
public class KeywordController {
    private final KeywordService keywordService;

    // 전체 키워드 조회
    @GetMapping
    public ResponseEntity<List<KeywordResponseDto>> getAllKeywords(
            @RequestParam(value = "type", required = false) KeywordType keywordType) {
        if (keywordType != null) {
            return ResponseEntity.ok(keywordService.getKeywordsByType(keywordType));
        }
        return ResponseEntity.ok(keywordService.getAllKeywords());
    }

    // 단건(특정 키워드) 조회
    @GetMapping("/{id}")
    public ResponseEntity<KeywordResponseDto> getKeyword(@PathVariable Long id) {
        return ResponseEntity.ok(keywordService.getKeywordById(id));
    }
}
