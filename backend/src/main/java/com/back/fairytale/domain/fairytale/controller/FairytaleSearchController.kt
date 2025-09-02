package com.back.fairytale.domain.fairytale.controller

import com.back.fairytale.domain.fairytale.dto.search.FairytaleSearchRequest
import com.back.fairytale.domain.fairytale.dto.search.FairytaleSearchResponse
import com.back.fairytale.domain.fairytale.service.FairytaleSearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "동화 검색 API", description = "동화를 검색하는 API입니다.")
@RestController
@RequestMapping("/api/fairytales")
class FairytaleSearchController(
    private val fairytaleSearchService: FairytaleSearchService
) {
    
    @Operation(summary = "동화 검색", description = "키워드로 동화를 검색합니다. 제목과 내용에서 검색됩니다.")
    @GetMapping("/search")
    fun searchFairytales(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "date") sortBy: String,
        @RequestParam(defaultValue = "all") scope: String
    ): ResponseEntity<Page<FairytaleSearchResponse>> {

        // 키워드 검증
        if (keyword.trim().isEmpty()) throw IllegalArgumentException("검색어를 입력해주세요.")

        // 검색 요청 객체 생성 및 검증
        val searchRequest = FairytaleSearchRequest(
            keyword = keyword.trim(),
            page = maxOf(0, page),
            size = when {
                size <= 0 -> 10
                size > 100 -> 100
                else -> size
            },
            sortBy = sortBy,
            scope = scope
        )

        val searchResult = fairytaleSearchService.search(searchRequest)

        // 결과 반환 (페이징 정보 포함)
        return ResponseEntity.ok(searchResult)
    }
}