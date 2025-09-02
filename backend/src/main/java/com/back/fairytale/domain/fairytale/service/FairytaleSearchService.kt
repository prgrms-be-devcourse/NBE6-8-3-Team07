package com.back.fairytale.domain.fairytale.service

import com.back.fairytale.domain.fairytale.dto.search.FairytaleSearchRequest
import com.back.fairytale.domain.fairytale.dto.search.FairytaleSearchResponse
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class FairytaleSearchService (
    private val fairytaleRepository: FairytaleRepository
) {
    
    private val logger = LoggerFactory.getLogger(FairytaleSearchService::class.java)
    
    fun search(request: FairytaleSearchRequest): Page<FairytaleSearchResponse> {
        val startTime = System.currentTimeMillis()
        
        // 키워드 검증
        val trimmedKeyword = request.keyword.trim()
        if (trimmedKeyword.isEmpty()) {
            throw IllegalArgumentException("검색어를 입력해주세요.")
        }
        
        if (trimmedKeyword.length < 2) {
            throw IllegalArgumentException("검색어는 2글자 이상 입력해주세요.")
        }
        
        // 페이지 정보 검증 및 생성
        val validatedPage = maxOf(0, request.page)
        val validatedSize = when {
            request.size <= 0 -> 10
            request.size > 100 -> 100 // 최대 100개로 제한
            else -> request.size
        }

        // 정렬은 네이티브 쿼리에서 직접 처리하므로 여기서는 검증만 수행
        when (request.sortBy.lowercase()) {
            "date", "createdat", "latest" -> { /* 유효한 정렬 옵션 */ }
            "title", "name" -> { /* 유효한 정렬 옵션 */ }
            else -> {
                logger.warn("알 수 없는 정렬 옵션: {}, 기본값(date) 사용", request.sortBy)
            }
        }

        val pageable = PageRequest.of(
            validatedPage,
            validatedSize
        )
        
        // 데이터베이스 검색
        val fairytalesPage = fairytaleRepository.searchByKeyword(trimmedKeyword, pageable)
        
        // Entity -> DTO 변환
        val responsePage = fairytalesPage.map { fairytale ->
            FairytaleSearchResponse.from(fairytale)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        logger.info("검색 완료 - 키워드: '{}', 소요시간: {}ms, 결과수: {}", 
                   trimmedKeyword, duration, responsePage.totalElements)
        
        return responsePage
    }
}