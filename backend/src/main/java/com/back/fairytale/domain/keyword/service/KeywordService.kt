package com.back.fairytale.domain.keyword.service

import com.back.fairytale.domain.keyword.dto.KeywordResponseDto
import com.back.fairytale.domain.keyword.entity.Keyword
import com.back.fairytale.domain.keyword.enums.KeywordType
import com.back.fairytale.domain.keyword.repository.KeywordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KeywordService(
    private val keywordRepository: KeywordRepository
) {

    // 모든 키워드 조회
    @Transactional(readOnly = true)
    fun getAllKeywords(): List<KeywordResponseDto> {
        return keywordRepository.findAll().map { KeywordResponseDto.fromEntity(it) }
    }

    // 타입별 키워드 조회 (Enum 타입으로 받음)
    @Transactional(readOnly = true)
    fun getKeywordsByType(keywordType: KeywordType?): List<KeywordResponseDto> {
        return keywordRepository.findByKeywordType(keywordType).map { KeywordResponseDto.fromEntity(it) }
    }

    // 단건(특정 키워드) 조회
    @Transactional(readOnly = true)
    fun getKeywordById(id: Long): KeywordResponseDto {
        val keyword = keywordRepository.findById(id)
            .orElseThrow { IllegalArgumentException("키워드가 존재하지 않습니다.") }
        return KeywordResponseDto.fromEntity(keyword)
    }

    // 키워드 유효성 검사
    fun validateKeyword(keyword: String) {
        // 욕설 필터링
        val badWords = listOf("욕설1", "욕설2", "부적절한단어")
        if (badWords.any { bad -> keyword.lowercase().contains(bad) }) {
            throw IllegalArgumentException("부적절한 키워드입니다.")
        }
    }

    @Transactional
    fun incrementUsageCountWithOptimisticLock(keywordId: Long) {
        val keyword = keywordRepository.findById(keywordId)
            .orElseThrow { IllegalArgumentException("키워드가 존재하지 않습니다.") }

        keyword.incrementUsageCount() // usage_count 1 증가
        keywordRepository.save(keyword) // @Version 필드로 동시성 체크
    }
}
