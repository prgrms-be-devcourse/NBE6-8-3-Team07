package com.back.fairytale.domain.keyword.controller

import com.back.fairytale.domain.keyword.dto.KeywordResponseDto
import com.back.fairytale.domain.keyword.enums.KeywordType
import com.back.fairytale.domain.keyword.service.KeywordService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/keywords")
class KeywordController(
    private val keywordService: KeywordService
) {

    // 전체 키워드 조회
    @GetMapping
    fun getAllKeywords(
        @RequestParam(value = "type", required = false) keywordType: KeywordType?
    ): ResponseEntity<List<KeywordResponseDto>> {
        return if (keywordType != null) {
            ResponseEntity.ok(keywordService.getKeywordsByType(keywordType))
        } else {
            ResponseEntity.ok(keywordService.getAllKeywords())
        }
    }

    // 단건(특정 키워드) 조회
    @GetMapping("/{id}")
    fun getKeyword(@PathVariable id: Long): ResponseEntity<KeywordResponseDto> {
        return ResponseEntity.ok(keywordService.getKeywordById(id))
    }
}
