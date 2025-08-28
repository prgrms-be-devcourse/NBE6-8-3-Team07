package com.back.fairytale.domain.comments.dto

import com.back.fairytale.domain.comments.entity.Comments
import java.time.LocalDateTime

data class CommentsResponse(
    val id: Long,
    val fairytaleId: Long,
    val nickname: String,
    val content: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        @JvmStatic
        fun from(comments: Comments): CommentsResponse {
            return CommentsResponse(
                id = comments.id ?: throw IllegalArgumentException("댓글 ID는 필수입니다."),
                fairytaleId = comments.fairytale.id,
                nickname = comments.user.nickname,
                content = comments.content,
                createdAt = comments.createdAt,
                updatedAt = comments.updatedAt
            )
        }
    }
}
