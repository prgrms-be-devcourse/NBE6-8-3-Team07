package com.back.fairytale.domain.comments.service

import com.back.BackendApplication
import com.back.fairytale.domain.comments.dto.CommentsRequest
import com.back.fairytale.domain.comments.dto.CommentsUpdateRequest
import com.back.fairytale.domain.comments.entity.Comments
import com.back.fairytale.domain.comments.repository.CommentsRepository
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import com.google.cloud.storage.Storage
import com.ninjasquad.springmockk.MockkBean
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [BackendApplication::class])
@ActiveProfiles("test")
@Transactional
class CommentsServiceIntegrationTest @Autowired constructor(
    private val commentsService: CommentsService,
    private val commentsRepository: CommentsRepository,
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
        commentsRepository.deleteAll()
        fairytaleRepository.deleteAll()
        userRepository.deleteAll()

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
    @DisplayName("댓글 작성 성공")
    fun createComments_Success() {
        // Given
        val request = CommentsRequest(
            fairytaleId = fairytale.id!!,
            content = "재미있는 동화네요!",
            parentId = null
        )

        // When
        val result = commentsService.createComments(request, user.id!!)

        // Then
        assertNotNull(result.id)
        assertEquals(fairytale.id, result.fairytaleId)
        assertEquals(user.nickname, result.nickname)
        assertEquals("재미있는 동화네요!", result.content)
        assertEquals(null, result.parentId)
        assertNotNull(result.createdAt)
        
        // 데이터베이스 확인
        val savedComment = commentsRepository.findById(result.id).orElse(null)
        assertNotNull(savedComment)
        assertEquals("재미있는 동화네요!", savedComment.content)
        assertTrue(savedComment.isParentComment())
    }

    @Test
    @DisplayName("대댓글 작성 성공")
    fun createReply_Success() {
        // Given - 부모 댓글 먼저 생성
        val parentComment = Comments(
            fairytale = fairytale,
            user = user,
            content = "부모 댓글"
        )
        commentsRepository.save(parentComment)

        val request = CommentsRequest(
            fairytaleId = fairytale.id!!,
            content = "대댓글입니다!",
            parentId = parentComment.id
        )

        // When
        val result = commentsService.createComments(request, otherUser.id!!)

        // Then
        assertNotNull(result.id)
        assertEquals(fairytale.id, result.fairytaleId)
        assertEquals(otherUser.nickname, result.nickname)
        assertEquals("대댓글입니다!", result.content)
        assertEquals(parentComment.id, result.parentId)
        
        // 데이터베이스 확인
        val savedReply = commentsRepository.findById(result.id).orElse(null)
        assertNotNull(savedReply)
        assertTrue(savedReply.isReply())
        assertEquals(1, savedReply.getDepth())
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 댓글 작성 실패")
    fun createComments_UserNotFound() {
        // Given
        val request = CommentsRequest(
            fairytaleId = fairytale.id!!,
            content = "댓글 내용",
            parentId = null
        )

        // When & Then
        assertThrows(EntityNotFoundException::class.java) {
            commentsService.createComments(request, 999L)
        }
    }

    @Test
    @DisplayName("존재하지 않는 동화로 댓글 작성 실패")
    fun createComments_FairytaleNotFound() {
        // Given
        val request = CommentsRequest(
            fairytaleId = 999L,
            content = "댓글 내용",
            parentId = null
        )

        // When & Then
        assertThrows(EntityNotFoundException::class.java) {
            commentsService.createComments(request, user.id!!)
        }
    }

    @Test
    @DisplayName("존재하지 않는 부모 댓글로 대댓글 작성 실패")
    fun createReply_ParentNotFound() {
        // Given
        val request = CommentsRequest(
            fairytaleId = fairytale.id!!,
            content = "대댓글 내용",
            parentId = 999L
        )

        // When & Then
        assertThrows(EntityNotFoundException::class.java) {
            commentsService.createComments(request, user.id!!)
        }
    }

    @Test
    @DisplayName("다른 동화의 부모 댓글로 대댓글 작성 실패")
    fun createReply_DifferentFairytale() {
        // Given - 다른 동화 생성
        val otherFairytale = Fairytale(
            user = otherUser,
            title = "다른 동화",
            content = "다른 내용",
            imageUrl = "www.example.com/other.jpg"
        )
        fairytaleRepository.save(otherFairytale)

        // 다른 동화에 부모 댓글 생성
        val parentComment = Comments(
            fairytale = otherFairytale,
            user = otherUser,
            content = "다른 동화의 댓글"
        )
        commentsRepository.save(parentComment)

        // 현재 동화에 대댓글 작성 시도
        val request = CommentsRequest(
            fairytaleId = fairytale.id!!,
            content = "대댓글 내용",
            parentId = parentComment.id
        )

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            commentsService.createComments(request, user.id!!)
        }
    }

    @Test
    @DisplayName("대댓글에 대댓글 작성 실패")
    fun createReply_ToReply_Failure() {
        // Given - 부모 댓글과 대댓글 생성
        val parentComment = Comments(
            fairytale = fairytale,
            user = user,
            content = "부모 댓글"
        )
        commentsRepository.save(parentComment)

        val replyComment = Comments(
            fairytale = fairytale,
            user = otherUser,
            content = "대댓글",
            parent = parentComment
        )
        
        // When & Then - 대댓글에 대댓글 작성 시도 시 엔티티 레벨에서 검증
        assertThrows(IllegalArgumentException::class.java) {
            Comments(
                fairytale = fairytale,
                user = user,
                content = "대댓글의 대댓글",
                parent = replyComment
            )
        }
    }

    @Test
    @DisplayName("동화별 댓글 조회 성공")
    fun getCommentsByFairytale_Success() {
        // Given - 부모 댓글과 대댓글들 생성
        val parentComment1 = Comments(
            fairytale = fairytale,
            user = user,
            content = "첫 번째 댓글"
        )
        commentsRepository.save(parentComment1)

        val parentComment2 = Comments(
            fairytale = fairytale,
            user = otherUser,
            content = "두 번째 댓글"
        )
        commentsRepository.save(parentComment2)

        val replyComment = Comments(
            fairytale = fairytale,
            user = user,
            content = "첫 번째 댓글의 대댓글",
            parent = parentComment1
        )
        commentsRepository.save(replyComment)

        val pageable = PageRequest.of(0, 10)

        // When
        val result = commentsService.getCommentsByFairytale(fairytale.id!!, pageable)

        // Then
        assertEquals(3, result.content.size)
        assertTrue(result.content.any { it.content == "첫 번째 댓글" && it.parentId == null })
        assertTrue(result.content.any { it.content == "두 번째 댓글" && it.parentId == null })
        assertTrue(result.content.any { it.content == "첫 번째 댓글의 대댓글" && it.parentId == parentComment1.id })
    }

    @Test
    @DisplayName("댓글 수정 성공")
    fun updateComments_Success() {
        // Given
        val comment = Comments(
            fairytale = fairytale,
            user = user,
            content = "원본 댓글"
        )
        commentsRepository.save(comment)

        val updateRequest = CommentsUpdateRequest(
            content = "수정된 댓글"
        )

        // When
        val result = commentsService.updateComments(comment.id!!, updateRequest, user.id!!)

        // Then
        assertEquals(comment.id, result.id)
        assertEquals("수정된 댓글", result.content)
        
        // 데이터베이스 확인
        val updatedComment = commentsRepository.findById(comment.id!!).orElse(null)
        assertNotNull(updatedComment)
        assertEquals("수정된 댓글", updatedComment.content)
    }

    @Test
    @DisplayName("다른 사용자가 댓글 수정 실패")
    fun updateComments_Unauthorized() {
        // Given
        val comment = Comments(
            fairytale = fairytale,
            user = user,
            content = "원본 댓글"
        )
        commentsRepository.save(comment)

        val updateRequest = CommentsUpdateRequest(
            content = "수정된 댓글"
        )

        // When & Then
        assertThrows(IllegalStateException::class.java) {
            commentsService.updateComments(comment.id!!, updateRequest, otherUser.id!!)
        }
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    fun deleteComments_Success() {
        // Given
        val comment = Comments(
            fairytale = fairytale,
            user = user,
            content = "삭제할 댓글"
        )
        commentsRepository.save(comment)
        val commentId = comment.id!!

        // When
        commentsService.deleteComments(commentId, user.id!!)

        // Then
        assertFalse(commentsRepository.findById(commentId).isPresent)
    }

    @Test
    @DisplayName("다른 사용자가 댓글 삭제 실패")
    fun deleteComments_Unauthorized() {
        // Given
        val comment = Comments(
            fairytale = fairytale,
            user = user,
            content = "댓글"
        )
        commentsRepository.save(comment)

        // When & Then
        assertThrows(IllegalStateException::class.java) {
            commentsService.deleteComments(comment.id!!, otherUser.id!!)
        }
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정 실패")
    fun updateComments_NotFound() {
        // Given
        val updateRequest = CommentsUpdateRequest(
            content = "수정된 댓글"
        )

        // When & Then
        assertThrows(EntityNotFoundException::class.java) {
            commentsService.updateComments(999L, updateRequest, user.id!!)
        }
    }

    @Test
    @DisplayName("존재하지 않는 댓글 삭제 실패")
    fun deleteComments_NotFound() {
        // When & Then
        assertThrows(EntityNotFoundException::class.java) {
            commentsService.deleteComments(999L, user.id!!)
        }
    }
}