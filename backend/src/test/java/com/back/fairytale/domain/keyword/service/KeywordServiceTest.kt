package com.back.fairytale.domain.keyword.service

import com.back.fairytale.domain.keyword.dto.KeywordResponseDto
import com.back.fairytale.domain.keyword.entity.Keyword
import com.back.fairytale.domain.keyword.enums.KeywordType
import com.back.fairytale.domain.keyword.repository.KeywordRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.given
import org.mockito.quality.Strictness
import java.util.*

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension::class)
class KeywordServiceTest {

    @Mock
    private lateinit var keywordRepository: KeywordRepository

    @InjectMocks
    private lateinit var keywordService: KeywordService

    @Test
    @DisplayName("전체 키워드 조회 - 매핑 확인")
    fun getAllKeywords() {
        // Given
        val keywords = listOf(
            Keyword.of("공주", KeywordType.CHARACTERS).copy(keywordId = 1L),
            Keyword.of("성", KeywordType.PLACE).copy(keywordId = 2L)
        )
        given(keywordRepository.findAll()).willReturn(keywords)

        // When
        val result: List<KeywordResponseDto> = keywordService.getAllKeywords()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].keyword).isEqualTo("공주")
        assertThat(result[0].keywordType).isEqualTo(KeywordType.CHARACTERS.name)
        assertThat(result[1].keyword).isEqualTo("성")
        assertThat(result[1].keywordType).isEqualTo(KeywordType.PLACE.name)
    }

    @Test
    @DisplayName("타입별 키워드 조회 - 필터 확인")
    fun getKeywordsByType() {
        // Given
        val type = KeywordType.CHARACTERS
        val keywords = listOf(
            Keyword.of("공주", type).copy(keywordId = 1L)
        )
        given(keywordRepository.findByKeywordType(type)).willReturn(keywords)

        // When
        val result = keywordService.getKeywordsByType(type)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].keyword).isEqualTo("공주")
        assertThat(result[0].keywordType).isEqualTo(type.name)
    }

    @Test
    @DisplayName("단건 조회 - 존재")
    fun getKeywordById_found() {
        // Given
        val keyword = Keyword.of("용", KeywordType.CHARACTERS).copy(keywordId = 1L)
        given(keywordRepository.findById(1L)).willReturn(Optional.of(keyword))

        // When
        val result = keywordService.getKeywordById(1L)

        // Then
        assertThat(result.keyword).isEqualTo("용")
        assertThat(result.keywordType).isEqualTo(KeywordType.CHARACTERS.name)
    }
}
