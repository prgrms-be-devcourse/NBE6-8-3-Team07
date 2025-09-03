package com.back.fairytale.domain.thoughts.service

import com.back.BackendApplication
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.thoughts.dto.ThoughtsRequest
import com.back.fairytale.domain.thoughts.dto.ThoughtsUpdateRequest
import com.back.fairytale.domain.thoughts.entity.Thoughts
import com.back.fairytale.domain.thoughts.repository.ThoughtsRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.domain.user.service.AuthService
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.jwt.JWTUtil
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import com.google.cloud.storage.Storage
import com.ninjasquad.springmockk.MockkBean
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [BackendApplication::class])
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ThoughtsServiceIntegrationTest @Autowired constructor(
    private val thoughtsService: ThoughtsService,
    private val thoughtsRepository: ThoughtsRepository,
    private val userRepository: UserRepository,
    private val fairytaleRepository: FairytaleRepository
) {

    // Spring Boot Test(Spring Context)에서 MockkBean을 사용하여 필요한 의존성 주입
    @MockkBean
    private lateinit var geminiClient: GeminiClient

    @MockkBean
    private lateinit var huggingFaceClient: HuggingFaceClient

    @MockkBean
    private lateinit var googleCloudStorage: GoogleCloudStorage

    @MockkBean
    private lateinit var storage: Storage

    private lateinit var user: User
    private lateinit var otherUser: User
    private lateinit var fairytale: Fairytale

    @BeforeEach
    fun setUp() {
        thoughtsRepository.deleteAllInBatch()
        fairytaleRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()

        user = User(
            email = "test@naver.com",
            name = "홍길동",
            nickname = "길동",
            socialId = "1234"
        )
        userRepository.save(user)

        otherUser = User(
            email = "other@naver.com",
            name = "김철수",
            nickname = "철수",
            socialId = "5678"
        )
        userRepository.save(otherUser)

        fairytale = Fairytale(
            user = user,
            title = "테스트 동화",
            content = "옛날 옛적에...",
            imageUrl = "www.example.com/image.jpg"
        )
        fairytaleRepository.save(fairytale)
    }

    @Test
    @DisplayName("아이생각 작성 성공")
    fun createThoughts_Success() {
        // Given
        val request = ThoughtsRequest(
            fairytaleId = fairytale.id!!,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )

        // When
        val result = thoughtsService.createThoughts(request, user.id!!)

        // Then
        assertNotNull(result.id)
        assertEquals(fairytale.id, result.fairytaleId)
        assertEquals(user.id, result.userId)
        assertEquals(user.name, result.userName)
        assertEquals("아이이름", result.name)
        assertEquals("아이생각 내용", result.content)
        assertEquals("부모생각 내용", result.parentContent)
        assertNotNull(result.createdAt)
        
        // 데이터베이스 확인
        val savedThoughts = thoughtsRepository.findById(result.id).orElse(null)
        assertNotNull(savedThoughts)
        assertEquals("아이이름", savedThoughts.name)
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 아이생각 작성 실패")
    fun createThoughts_UserNotFound() {
        // Given
        val request = ThoughtsRequest(
            fairytaleId = fairytale.id!!,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )

        // When & Then
        assertThrows(EntityNotFoundException::class.java) {
            thoughtsService.createThoughts(request, 999L)
        }
    }

    @Test
    @DisplayName("존재하지 않는 동화로 아이생각 작성 실패")
    fun createThoughts_FairytaleNotFound() {
        // Given
        val request = ThoughtsRequest(
            fairytaleId = 999L,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )

        // When & Then
        assertThrows(EntityNotFoundException::class.java) {
            thoughtsService.createThoughts(request, user.id!!)
        }
    }

    @Test
    @DisplayName("아이생각 조회 성공")
    fun getThoughts_Success() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        // When
        val result = thoughtsService.getThoughts(thoughts.id!!, user.id!!)

        // Then
        assertEquals(thoughts.id, result.id)
        assertEquals(fairytale.id, result.fairytaleId)
        assertEquals(user.id, result.userId)
        assertEquals("아이이름", result.name)
        assertEquals("아이생각 내용", result.content)
        assertEquals("부모생각 내용", result.parentContent)
    }

    @Test
    @DisplayName("다른 사용자의 아이생각 조회 실패")
    fun getThoughts_Unauthorized() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        // When & Then
        assertThrows(IllegalStateException::class.java) {
            thoughtsService.getThoughts(thoughts.id!!, otherUser.id!!)
        }
    }

    @Test
    @DisplayName("존재하지 않는 아이생각 조회 실패")
    fun getThoughts_NotFound() {
        // When & Then
        assertThrows(EntityNotFoundException::class.java) {
            thoughtsService.getThoughts(999L, user.id!!)
        }
    }

    @Test
    @DisplayName("동화별 아이생각 조회 성공")
    fun getThoughtsByFairytaleId_Success() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        // When
        val result = thoughtsService.getThoughtsByFairytaleId(fairytale.id!!, user.id!!)

        // Then
        assertEquals(thoughts.id, result.id)
        assertEquals(fairytale.id, result.fairytaleId)
        assertEquals(user.id, result.userId)
        assertEquals("아이이름", result.name)
    }

    @Test
    @DisplayName("동화별 아이생각 조회 - 존재하지 않는 경우")
    fun getThoughtsByFairytaleId_NotFound() {
        // When & Then
        assertThrows(EntityNotFoundException::class.java) {
            thoughtsService.getThoughtsByFairytaleId(fairytale.id!!, user.id!!)
        }
    }

    @Test
    @DisplayName("아이생각 수정 성공")
    fun updateThoughts_Success() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        val updateRequest = ThoughtsUpdateRequest(
            name = "수정된 아이이름",
            content = "수정된 아이생각 내용",
            parentContent = "수정된 부모생각 내용"
        )

        // When
        val result = thoughtsService.updateThoughts(thoughts.id!!, updateRequest, user.id!!)

        // Then
        assertEquals(thoughts.id, result.id)
        assertEquals("수정된 아이이름", result.name)
        assertEquals("수정된 아이생각 내용", result.content)
        assertEquals("수정된 부모생각 내용", result.parentContent)
        
        // 데이터베이스 확인
        val updatedThoughts = thoughtsRepository.findById(thoughts.id!!).orElse(null)
        assertNotNull(updatedThoughts)
        assertEquals("수정된 아이이름", updatedThoughts.name)
        assertEquals("수정된 아이생각 내용", updatedThoughts.content)
        assertEquals("수정된 부모생각 내용", updatedThoughts.parentContent)
    }

    @Test
    @DisplayName("다른 사용자가 아이생각 수정 실패")
    fun updateThoughts_Unauthorized() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        val updateRequest = ThoughtsUpdateRequest(
            name = "수정된 아이이름",
            content = "수정된 아이생각 내용",
            parentContent = "수정된 부모생각 내용"
        )

        // When & Then
        assertThrows(IllegalStateException::class.java) {
            thoughtsService.updateThoughts(thoughts.id!!, updateRequest, otherUser.id!!)
        }
    }

    @Test
    @DisplayName("아이생각 삭제 성공")
    fun deleteThoughts_Success() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)
        val thoughtsId = thoughts.id!!

        // When
        thoughtsService.deleteThoughts(thoughtsId, user.id!!)

        // Then
        assertFalse(thoughtsRepository.findById(thoughtsId).isPresent)
    }

    @Test
    @DisplayName("다른 사용자가 아이생각 삭제 실패")
    fun deleteThoughts_Unauthorized() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        // When & Then
        assertThrows(IllegalStateException::class.java) {
            thoughtsService.deleteThoughts(thoughts.id!!, otherUser.id!!)
        }
    }

    @Test
    @DisplayName("존재하지 않는 아이생각 삭제 실패")
    fun deleteThoughts_NotFound() {
        // When & Then
        assertThrows(EntityNotFoundException::class.java) {
            thoughtsService.deleteThoughts(999L, user.id!!)
        }
    }
}