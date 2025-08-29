package com.back.fairytale.domain.comments.service

import com.back.fairytale.domain.comments.dto.CommentsRequest
import com.back.fairytale.domain.comments.dto.CommentsUpdateRequest
import com.back.fairytale.domain.comments.entity.Comments
import com.back.fairytale.domain.comments.repository.CommentsRepository
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
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
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.*
import org.mockito.quality.Strictness
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.*

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension::class)
class CommentsServiceTest {

    @Mock
    private lateinit var commentsRepository: CommentsRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var fairytaleRepository: FairytaleRepository

    @InjectMocks
    private lateinit var commentsService: CommentsService

    private fun createMockUser(id: Long, name: String): User {
        return mock {
            on { this.id } doReturn id
            on { this.name } doReturn name
            on { this.nickname } doReturn name
        }
    }

    private fun createMockFairytale(id: Long): Fairytale {
        return mock { on { this.id } doReturn id }
    }

    private fun createMockComment(id: Long, user: User, fairytale: Fairytale, content: String): Comments {
        return mock {
            on { this.id } doReturn id
            on { this.user } doReturn user
            on { this.fairytale } doReturn fairytale
            on { this.content } doReturn content
        }
    }

    @Test
    @DisplayName("[성공] 댓글 작성")
    fun createComments_Success() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockFairytale = createMockFairytale(10L)
        val request = CommentsRequest(mockFairytale.id!!, "정말 재미있는 동화였어요!")
        val savedMockComment = createMockComment(1L, mockUser, mockFairytale, request.content)

        given(userRepository.findById(mockUser.id!!)).willReturn(Optional.of(mockUser))
        given(fairytaleRepository.findById(mockFairytale.id!!)).willReturn(Optional.of(mockFairytale))
        given(commentsRepository.save(any<Comments>())).willReturn(savedMockComment)

        // When
        val response = commentsService.createComments(request, mockUser.id!!)

        // Then
        assertThat(response.content).isEqualTo(request.content)
        verify(commentsRepository, times(1)).save(any<Comments>())
    }

    @Test
    @DisplayName("[실패] 댓글 작성 - 유저를 찾을 수 없음")
    fun createComments_Fail_UserNotFound() {
        // Given
        val mockFairytale = createMockFairytale(10L)
        val request = CommentsRequest(mockFairytale.id!!, "이 댓글은 작성될 수 없어요.")

        given(userRepository.findById(any())).willReturn(Optional.empty())

        // When & Then
        assertThrows<EntityNotFoundException> {
            commentsService.createComments(request, 99L)
        }
    }

    @Test
    @DisplayName("[성공] 동화별 댓글 목록 조회")
    fun getCommentsByFairytale_Success() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockFairytale = createMockFairytale(10L)
        val pageable = PageRequest.of(0, 10)
        val commentsList = listOf(
            createMockComment(1L, mockUser, mockFairytale, "댓글 1"),
            createMockComment(2L, mockUser, mockFairytale, "댓글 2")
        )
        val commentsPage = PageImpl(commentsList, pageable, commentsList.size.toLong())

        given(fairytaleRepository.findById(mockFairytale.id!!)).willReturn(Optional.of(mockFairytale))
        given(commentsRepository.findByFairytale(mockFairytale, pageable)).willReturn(commentsPage)

        // When
        val response = commentsService.getCommentsByFairytale(mockFairytale.id!!, pageable)

        // Then
        assertThat(response.content.size).isEqualTo(2)
        assertThat(response.content[0].content).isEqualTo("댓글 1")
    }

    @Test
    @DisplayName("[성공] 댓글 수정")
    fun updateComments_Success() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockFairytale = createMockFairytale(10L)
        val commentId = 100L
        val request = CommentsUpdateRequest("수정된 댓글 내용입니다.")
        val originalComment = createMockComment(commentId, mockUser, mockFairytale, "원본 댓글 내용")

        given(commentsRepository.findById(commentId)).willReturn(Optional.of(originalComment))

        // When
        commentsService.updateComments(commentId, request, mockUser.id!!)

        // Then
        // updateContent 메소드가 올바른 내용으로 호출되었는지 여부만 검증합니다.
        verify(originalComment, times(1)).updateContent(request.content)
    }

    @Test
    @DisplayName("[실패] 댓글 수정 - 작성자가 아님")
    fun updateComments_Fail_Unauthorized() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockOtherUser = createMockUser(2L, "다른유저")
        val mockFairytale = createMockFairytale(10L)
        val commentId = 100L
        val originalComment = createMockComment(commentId, mockUser, mockFairytale, "원본 댓글 내용")

        given(commentsRepository.findById(commentId)).willReturn(Optional.of(originalComment))

        // When & Then
        assertThrows<IllegalStateException> {
            commentsService.updateComments(commentId, CommentsUpdateRequest("..."), mockOtherUser.id!!)
        }
    }

    @Test
    @DisplayName("[성공] 댓글 삭제")
    fun deleteComments_Success() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockFairytale = createMockFairytale(10L)
        val commentId = 100L
        val mockComment = createMockComment(commentId, mockUser, mockFairytale, "삭제될 댓글")

        given(commentsRepository.findById(commentId)).willReturn(Optional.of(mockComment))

        // When
        commentsService.deleteComments(commentId, mockUser.id!!)

        // Then
        verify(commentsRepository, times(1)).delete(mockComment)
    }

    @Test
    @DisplayName("[실패] 댓글 삭제 - 작성자가 아님")
    fun deleteComments_Fail_Unauthorized() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockOtherUser = createMockUser(2L, "다른유저")
        val mockFairytale = createMockFairytale(10L)
        val commentId = 100L
        val mockComment = createMockComment(commentId, mockUser, mockFairytale, "삭제될 댓글")

        given(commentsRepository.findById(commentId)).willReturn(Optional.of(mockComment))

        // When & Then
        assertThrows<IllegalStateException> {
            commentsService.deleteComments(commentId, mockOtherUser.id!!)
        }
    }
}
