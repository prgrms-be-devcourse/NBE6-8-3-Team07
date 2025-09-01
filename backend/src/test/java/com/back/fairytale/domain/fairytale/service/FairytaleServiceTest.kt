package com.back.fairytale.domain.fairytale.service

import com.back.fairytale.domain.fairytale.exception.FairytaleNotFoundException
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.keyword.repository.KeywordRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import com.google.cloud.storage.Storage
import com.ninjasquad.springmockk.MockkBean
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FairytaleServiceIntegrationTest {

    @Autowired lateinit var fairytaleService: FairytaleService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var fairytaleRepository: FairytaleRepository
    @Autowired lateinit var keywordRepository: KeywordRepository

    // 외부 의존은 Mock 으로 대체 (서비스 빈 주입용)
    @MockkBean lateinit var geminiClient: GeminiClient
    @MockkBean lateinit var huggingFaceClient: HuggingFaceClient
    @MockkBean lateinit var googleCloudStorage: GoogleCloudStorage
    @MockkBean lateinit var storage: Storage

    private var savedUserId: Long = -1

    @BeforeEach
    fun setUpUser() {
        val saved = userRepository.save(
            User(
                name = "tester",
                nickname = "nick",
                email = "tester@example.com",
                socialId = "social"
            )
        )
        savedUserId = saved.id!!
    }

    @Test
    @DisplayName("getAllFairytalesByUserId: 동화 없음")
    fun t1() {
        assertThatThrownBy {
            fairytaleService.getAllFairytalesByUserId(savedUserId)
        }
            .isInstanceOf(FairytaleNotFoundException::class.java)
            .hasMessageContaining("등록된 동화가 없습니다.")
    }
}
