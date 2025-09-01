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
    val parentId: Long? = null, // 부모 댓글 ID
    val depth: Int, // 댓글 깊이 (0: 부모 댓글, 1: 대댓글)
    val hasChildren: Boolean, // 자식 댓글 존재 여부
    val childrenCount: Int, // 자식 댓글 수
) {
    companion object {
        @JvmStatic
        fun from(comments: Comments): CommentsResponse {
            return CommentsResponse(
                id = comments.id ?: throw IllegalArgumentException("댓글 ID는 필수입니다."),
                fairytaleId = comments.fairytale.id ?: throw IllegalArgumentException("동화 ID는 필수입니다."),
                nickname = comments.user.nickname,
                content = comments.content,
                createdAt = comments.createdAt,
                updatedAt = comments.updatedAt,
                parentId = comments.parent?.id,
                depth = comments.getDepth(),
                hasChildren = comments.hasChildren(),
                childrenCount = comments.children.size,
            )
        }

        @JvmStatic
        fun from(comments: Comments, childrenCount: Long): CommentsResponse { // childrenCount를 외부에서 주입위한 오버로딩
            return CommentsResponse(
                id = comments.id ?: throw IllegalArgumentException("댓글 ID는 필수입니다."),
                fairytaleId = comments.fairytale.id ?: throw IllegalArgumentException("동화 ID는 필수입니다."),
                nickname = comments.user.nickname,
                content = comments.content,
                createdAt = comments.createdAt,
                updatedAt = comments.updatedAt,
                parentId = comments.parent?.id,
                depth = comments.getDepth(),
                hasChildren = comments.hasChildren(),
                childrenCount = childrenCount.toInt() // 외부에서 계산된 값 사용
            )
        }
    }
}
