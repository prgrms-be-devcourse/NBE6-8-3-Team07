package com.back.fairytale.domain.comments.service

import com.back.fairytale.domain.comments.dto.CommentsRequest
import com.back.fairytale.domain.comments.dto.CommentsResponse
import com.back.fairytale.domain.comments.dto.CommentsResponse.Companion.from
import com.back.fairytale.domain.comments.dto.CommentsUpdateRequest
import com.back.fairytale.domain.comments.entity.Comments
import com.back.fairytale.domain.comments.repository.CommentsRepository
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentsService (
    private val commentsRepository: CommentsRepository,
    private val userRepository: UserRepository,
    private val fairytaleRepository: FairytaleRepository,
) {
    // 공통 로직
    // 로깅 로직
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(CommentsService::class.java)
    }
    // 댓글 조회 및 유저 확인
    private fun findCommentAndCheckUser(id: Long, userId: Long): Comments {
        // 댓글 조회
        val comments = commentsRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Id가 $id 인 댓글을 찾을 수 없습니다.") }

        // 유저 확인
        if (comments.user.id != userId) {
            throw IllegalStateException("접근 권한이 없습니다. 댓글 작성자와 요청한 유저가 일치하지 않습니다.")
        }

        return comments
    }

    // 댓글 작성
    fun createComments(request: CommentsRequest, userId: Long): CommentsResponse {
        // 유저와 동화 조회
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("Id가 $id 인 댓글을 찾을 수 없습니다.") }
        val fairytale = fairytaleRepository.findById(request.fairytaleId)
            .orElseThrow { EntityNotFoundException("Id가 ${request.fairytaleId} 인 동화를 찾을 수 없습니다.") }

        // 댓글 생성
        val comments = Comments(fairytale, user, request.content)

        // 댓글 저장
        val savedComments = commentsRepository.save(comments)

        // 응답 생성
        return from(savedComments)
    }

    // 댓글 조회
    @Transactional(readOnly = true)
    fun getCommentsByFairytale(fairytaleId: Long, pageable: Pageable): Page<CommentsResponse> {
        // 동화 조회
        val fairytale = fairytaleRepository.findById(fairytaleId)
            .orElseThrow { EntityNotFoundException("Id가 $fairytaleId 인 동화를 찾을 수 없습니다.") }

        // 댓글 조회
        val commentsPage: Page<Comments> = commentsRepository.findByFairytale(fairytale, pageable)

        // 응답 생성
        return commentsPage.map(CommentsResponse::from)
    }

    // 댓글 수정
    fun updateComments(id: Long, request: CommentsUpdateRequest, userId: Long): CommentsResponse {
        // 댓글 조회 및 유저 확인
        val comments = findCommentAndCheckUser(id, userId)

        // 댓글 수정
        comments.updateContent(request.content)

        // 응답 생성
        return from(comments)
    }

    // 댓글 삭제
    fun deleteComments(id: Long, userId: Long) {
        // 댓글 조회 및 유저 확인
        val comments = findCommentAndCheckUser(id, userId)

        // 댓글 삭제
        commentsRepository.delete(comments)

        log.info("댓글이 성공적으로 삭제되었습니다. ID: $id")
    }
}
