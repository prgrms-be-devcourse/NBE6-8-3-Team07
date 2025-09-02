package com.back.fairytale.domain.keyword.service

import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.keyword.dto.KeywordResponseDto
import com.back.fairytale.domain.keyword.entity.Keyword
import com.back.fairytale.domain.keyword.enums.KeywordType
import com.back.fairytale.domain.keyword.repository.KeywordRepository
import com.back.fairytale.domain.like.repository.LikeRepository
import com.back.fairytale.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class KeywordServiceIntegrationTest {

    @Autowired
    private lateinit var keywordService: KeywordService

    @Autowired
    private lateinit var keywordRepository: KeywordRepository

    @Autowired
    private lateinit var fairytaleRepository: FairytaleRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var likeRepository: LikeRepository

    @BeforeEach
    fun setUp() {
        // 참조하는 쪽(자식)의 데이터를 먼저 삭제해야 합니다.
        likeRepository.deleteAll()

        // 그 다음 부모 데이터를 삭제합니다.
        // Fairytale 삭제 시 Cascade 옵션에 의해 FairytaleKeyword, Bookmark 등도 함께 삭제됩니다.
        fairytaleRepository.deleteAll()
        userRepository.deleteAll()
        keywordRepository.deleteAll()
    }

    @Test
    @DisplayName("전체 키워드 조회 - 통합테스트")
    fun getAllKeywords() {
        // Given
        val keyword1 = Keyword.of("공주", KeywordType.CHARACTERS)
        val keyword2 = Keyword.of("성", KeywordType.PLACE)
        keywordRepository.saveAll(listOf(keyword1, keyword2))

        // When
        val result: List<KeywordResponseDto> = keywordService.getAllKeywords()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result).extracting("keyword").containsExactlyInAnyOrder("공주", "성")
        assertThat(result).extracting("keywordType")
            .containsExactlyInAnyOrder(
                KeywordType.CHARACTERS.name,
                KeywordType.PLACE.name
            )
    }

    @Test
    @DisplayName("타입별 키워드 조회 - 통합테스트")
    fun getKeywordsByType() {
        // Given
        val type = KeywordType.CHARACTERS
        val keyword1 = Keyword.of("공주", type)
        val keyword2 = Keyword.of("왕자", type)
        val keyword3 = Keyword.of("숲", KeywordType.PLACE) // 다른 타입의 키워드
        keywordRepository.saveAll(listOf(keyword1, keyword2, keyword3))

        // When
        val result = keywordService.getKeywordsByType(type)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result).extracting("keyword").containsExactlyInAnyOrder("공주", "왕자")
        assertThat(result).allMatch { it.keywordType == type.name }
    }

    @Test
    @DisplayName("단건 조회 - 존재하는 키워드 - 통합테스트")
    fun getKeywordById_found() {
        // Given
        val keyword = keywordRepository.save(Keyword.of("용", KeywordType.CHARACTERS))
        val keywordId = keyword.keywordId!!

        // When
        val result = keywordService.getKeywordById(keywordId)

        // Then
        assertThat(result.keyword).isEqualTo("용")
        assertThat(result.keywordType).isEqualTo(KeywordType.CHARACTERS.name)
    }

    @Test
    @DisplayName("키워드 검증 - 정상 키워드 - 통합테스트")
    fun validateKeyword_valid() {
        // Given
        val validKeyword = "공주"
        keywordRepository.save(Keyword.of(validKeyword, KeywordType.CHARACTERS))

        // When & Then
        // 현재 로직에서는 예외가 발생하지 않는지만 확인합니다.
        keywordService.validateKeyword(validKeyword)
    }
}
