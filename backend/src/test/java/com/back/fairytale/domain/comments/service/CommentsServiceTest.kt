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

    private fun createMockComment(id: Long, user: User, fairytale: Fairytale, content: String, parent: Comments? = null): Comments {
        return mock {
            on { this.id } doReturn id
            on { this.user } doReturn user
            on { this.fairytale } doReturn fairytale
            on { this.content } doReturn content
            on { this.parent } doReturn parent
            on { this.getDepth() } doReturn (if (parent == null) 0 else 1)
            on { this.hasChildren() } doReturn false
            on { this.children } doReturn mutableListOf()
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

    // 대댓글 관련 테스트 케이스
    @Test
    @DisplayName("[성공] 대댓글 작성")
    fun createReply_Success() {
        // Given
        val mockUser = createMockUser(1L, "대댓글작성자")
        val mockFairytale = createMockFairytale(10L)
        val parentComment = createMockComment(1L, mockUser, mockFairytale, "부모 댓글")
        val replyRequest = CommentsRequest(mockFairytale.id!!, "대댓글 내용", parentComment.id)
        val savedReply = createMockComment(2L, mockUser, mockFairytale, "대댓글 내용", parentComment)

        given(userRepository.findById(mockUser.id!!)).willReturn(Optional.of(mockUser))
        given(fairytaleRepository.findById(mockFairytale.id!!)).willReturn(Optional.of(mockFairytale))
        given(commentsRepository.findById(parentComment.id!!)).willReturn(Optional.of(parentComment))
        given(commentsRepository.save(any<Comments>())).willReturn(savedReply)

        // When
        val response = commentsService.createComments(replyRequest, mockUser.id!!)

        // Then
        assertThat(response.content).isEqualTo("대댓글 내용")
        assertThat(response.parentId).isEqualTo(parentComment.id)
        assertThat(response.depth).isEqualTo(1)
        verify(commentsRepository, times(1)).save(any<Comments>())
    }

    @Test
    @DisplayName("[실패] 대댓글 작성 - 부모 댓글을 찾을 수 없음")
    fun createReply_Fail_ParentNotFound() {
        // Given
        val mockUser = createMockUser(1L, "대댓글작성자")
        val mockFairytale = createMockFairytale(10L)
        val replyRequest = CommentsRequest(mockFairytale.id!!, "대댓글 내용", 999L)

        given(userRepository.findById(mockUser.id!!)).willReturn(Optional.of(mockUser))
        given(fairytaleRepository.findById(mockFairytale.id!!)).willReturn(Optional.of(mockFairytale))
        given(commentsRepository.findById(999L)).willReturn(Optional.empty())

        // When & Then
        assertThrows<EntityNotFoundException> {
            commentsService.createComments(replyRequest, mockUser.id!!)
        }
    }

    @Test
    @DisplayName("[실패] 대댓글 작성 - 다른 동화의 댓글에 대댓글 시도")
    fun createReply_Fail_DifferentFairytale() {
        // Given
        val mockUser = createMockUser(1L, "대댓글작성자")
        val mockFairytale1 = createMockFairytale(10L)
        val mockFairytale2 = createMockFairytale(20L)
        val parentComment = createMockComment(1L, mockUser, mockFairytale2, "다른 동화의 댓글")
        val replyRequest = CommentsRequest(mockFairytale1.id!!, "대댓글 내용", parentComment.id)

        given(userRepository.findById(mockUser.id!!)).willReturn(Optional.of(mockUser))
        given(fairytaleRepository.findById(mockFairytale1.id!!)).willReturn(Optional.of(mockFairytale1))
        given(commentsRepository.findById(parentComment.id!!)).willReturn(Optional.of(parentComment))

        // When & Then
        assertThrows<IllegalArgumentException> {
            commentsService.createComments(replyRequest, mockUser.id!!)
        }
    }

    @Test
    @DisplayName("[성공] 계층구조 댓글 조회 (N+1 최적화)")
    fun getCommentsByFairytale_WithHierarchy_Success() {
        // Given
        val mockUser = createMockUser(1L, "테스트유저")
        val mockFairytale = createMockFairytale(10L)
        val pageable = PageRequest.of(0, 10)
        
        val parentComment = createMockComment(1L, mockUser, mockFairytale, "부모 댓글")
        val replyComment1 = createMockComment(2L, mockUser, mockFairytale, "대댓글1", parentComment)
        val replyComment2 = createMockComment(3L, mockUser, mockFairytale, "대댓글2", parentComment)
        
        val commentsList = listOf(parentComment, replyComment1, replyComment2)
        val commentsPage = PageImpl(commentsList, pageable, commentsList.size.toLong())
        
        // N+1 방지를 위한 childrenCount Map 시뮬레이션
        val childrenCountResults = listOf(arrayOf<Any>(1L, 2L)) // parentId=1, count=2
        
        given(commentsRepository.findByFairytaleIdOrderByHierarchy(mockFairytale.id!!, pageable))
            .willReturn(commentsPage)
        given(commentsRepository.countChildrenByParentId(listOf(1L)))
            .willReturn(childrenCountResults)

        // When
        val response = commentsService.getCommentsByFairytale(mockFairytale.id!!, pageable)

        // Then
        assertThat(response.content.size).isEqualTo(3)
        assertThat(response.content[0].parentId).isNull()
        assertThat(response.content[0].depth).isEqualTo(0)
        assertThat(response.content[1].parentId).isEqualTo(1L)
        assertThat(response.content[1].depth).isEqualTo(1)
        assertThat(response.content[2].parentId).isEqualTo(1L)
        assertThat(response.content[2].depth).isEqualTo(1)
        
        // Repository 메서드 호출 검증
        verify(commentsRepository, times(1)).findByFairytaleIdOrderByHierarchy(mockFairytale.id!!, pageable)
        verify(commentsRepository, times(1)).countChildrenByParentId(listOf(1L))
    }
}
