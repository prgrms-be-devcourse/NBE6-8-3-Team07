package com.back.fairytale.domain.thoughts.service

import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.thoughts.dto.ThoughtsRequest
import com.back.fairytale.domain.thoughts.dto.ThoughtsUpdateRequest
import com.back.fairytale.domain.thoughts.entity.Thoughts
import com.back.fairytale.domain.thoughts.repository.ThoughtsRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.*

import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension::class)
class ThoughtsServiceTest {

    @Mock
    private lateinit var thoughtsRepository: ThoughtsRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var fairytaleRepository: FairytaleRepository

    @InjectMocks
    private lateinit var thoughtsService: ThoughtsService

    // --- Helper Functions to create Mocks ---
    private fun createMockUser(id: Long, name: String): User {
        return mock {
            on { this.id } doReturn id
            on { this.name } doReturn name // 'userName' NPE 방지를 위해 name 속성 stubbing
            on { this.nickname } doReturn name
        }
    }

    private fun createMockFairytale(id: Long): Fairytale {
        return mock { on { this.id } doReturn id }
    }

    private fun createMockThought(id: Long, user: User, fairytale: Fairytale, content: String = "아이생각 내용"): Thoughts {
        return mock {
            on { this.id } doReturn id
            on { this.user } doReturn user
            on { this.fairytale } doReturn fairytale
            on { this.name } doReturn "아이이름"
            on { this.content } doReturn content
            on { this.parentContent } doReturn "부모생각 내용"
        }
    }

    // --- Test Cases ---

    @Test
    @DisplayName("[성공] 아이생각 작성")
    fun createThoughts_Success() {
        // Given: 각 테스트에 필요한 mock 객체를 테스트 내부에서 생성
        val mockUser = createMockUser(1L, "테스트유저")
        val mockFairytale = createMockFairytale(10L)
        val request = ThoughtsRequest(mockFairytale.id!!, "이름", "내용", "부모내용")
        val savedMockThought = createMockThought(1L, mockUser, mockFairytale)

        given(userRepository.findById(mockUser.id!!)).willReturn(Optional.of(mockUser))
        given(fairytaleRepository.findById(mockFairytale.id!!)).willReturn(Optional.of(mockFairytale))
        given(thoughtsRepository.save(any<Thoughts>())).willReturn(savedMockThought)

        // When
        val response = thoughtsService.createThoughts(request, mockUser.id!!)

        // Then
        assertThat(response.name).isEqualTo(savedMockThought.name)
        assertThat(response.content).isEqualTo(savedMockThought.content)
        verify(thoughtsRepository, times(1)).save(any<Thoughts>())
    }

    @Test
    @DisplayName("[실패] 아이생각 작성 - 유저 없음")
    fun createThoughts_Fail_UserNotFound() {
        // Given
        val mockFairytale = createMockFairytale(10L)
        val request = ThoughtsRequest(mockFairytale.id!!, "이름", "내용", "부모내용")
        given(userRepository.findById(any())).willReturn(Optional.empty())

        // When & Then
        assertThrows<EntityNotFoundException> {
            thoughtsService.createThoughts(request, 99L)
        }
    }

    @Test
    @DisplayName("[성공] 아이생각 조회")
    fun getThoughts_Success() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockFairytale = createMockFairytale(10L)
        val thoughtId = 1L
        val mockThought = createMockThought(thoughtId, mockUser, mockFairytale)

        given(thoughtsRepository.findById(thoughtId)).willReturn(Optional.of(mockThought))

        // When
        val response = thoughtsService.getThoughts(thoughtId, mockUser.id!!)

        // Then
        assertThat(response.id).isEqualTo(thoughtId)
        assertThat(response.name).isEqualTo(mockThought.name)
    }

    @Test
    @DisplayName("[실패] 아이생각 조회 - 작성자가 아님")
    fun getThoughts_Fail_Unauthorized() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockOtherUser = createMockUser(2L, "다른유저")
        val mockFairytale = createMockFairytale(10L)
        val thoughtId = 1L
        val mockThought = createMockThought(thoughtId, mockUser, mockFairytale)

        given(thoughtsRepository.findById(thoughtId)).willReturn(Optional.of(mockThought))

        // When & Then
        assertThrows<IllegalStateException> {
            thoughtsService.getThoughts(thoughtId, mockOtherUser.id!!)
        }
    }

    @Test
    @DisplayName("[성공] 아이생각 수정")
    fun updateThoughts_Success() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockFairytale = createMockFairytale(10L)
        val thoughtId = 1L
        val request = ThoughtsUpdateRequest("수정된 이름", "수정된 내용", "수정된 부모내용")
        val mockThought = createMockThought(thoughtId, mockUser, mockFairytale)

        given(thoughtsRepository.findById(thoughtId)).willReturn(Optional.of(mockThought))

        // When
        thoughtsService.updateThoughts(thoughtId, request, mockUser.id!!)

        // Then
        verify(mockThought, times(1)).update(request.name, request.content, request.parentContent)
    }

    @Test
    @DisplayName("[성공] 아이생각 삭제")
    fun deleteThoughts_Success() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockFairytale = createMockFairytale(10L)
        val thoughtId = 1L
        val mockThought = createMockThought(thoughtId, mockUser, mockFairytale)

        given(thoughtsRepository.findById(thoughtId)).willReturn(Optional.of(mockThought))

        // When
        thoughtsService.deleteThoughts(thoughtId, mockUser.id!!)

        // Then
        verify(thoughtsRepository, times(1)).delete(mockThought)
    }
    
    @Test
    @DisplayName("[성공] 동화 ID로 아이생각 조회")
    fun getThoughtsByFairytaleId_Success() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockFairytale = createMockFairytale(10L)
        val thoughtId = 1L
        val mockThought = createMockThought(thoughtId, mockUser, mockFairytale)

        given(thoughtsRepository.findByFairytaleIdAndUserId(mockFairytale.id!!, mockUser.id!!)).willReturn(mockThought)

        // When
        val response = thoughtsService.getThoughtsByFairytaleId(mockFairytale.id!!, mockUser.id!!)

        // Then
        assertThat(response.id).isEqualTo(thoughtId)
        assertThat(response.name).isEqualTo(mockThought.name)
    }

    @Test
    @DisplayName("[실패] 동화 ID로 아이생각 조회 - 아이생각 없음")
    fun getThoughtsByFairytaleId_Fail_NotFound() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockFairytale = createMockFairytale(10L)

        given(thoughtsRepository.findByFairytaleIdAndUserId(mockFairytale.id!!, mockUser.id!!)).willReturn(null)

        // When & Then
        assertThrows<EntityNotFoundException> {
            thoughtsService.getThoughtsByFairytaleId(mockFairytale.id!!, mockUser.id!!)
        }
    }
}
