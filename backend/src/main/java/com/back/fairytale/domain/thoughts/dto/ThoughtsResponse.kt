package com.back.fairytale.domain.thoughts.dto

import com.back.fairytale.domain.thoughts.entity.Thoughts
import java.time.LocalDateTime

data class ThoughtsResponse(
    val id: Long,
    val fairytaleId: Long,
    val userId: Long,
    val userName: String,
    val name: String,
    val content: String,
    val parentContent: String,
    val createdAt: LocalDateTime?,
) {
    companion object {
        @JvmStatic
        fun from(thoughts: Thoughts): ThoughtsResponse {
            return ThoughtsResponse(
                id = thoughts.id ?: throw IllegalArgumentException("아이생각기록 ID는 필수입니다."),
                fairytaleId = thoughts.fairytale.id ?: throw IllegalArgumentException("동화 ID는 필수입니다."),
                userId = thoughts.user.id ?: throw IllegalArgumentException("사용자 ID는 필수입니다."),
                userName = thoughts.user.name,
                name = thoughts.name,
                content = thoughts.content,
                parentContent = thoughts.parentContent,
                createdAt = thoughts.createdAt,
            )
        }
    }
}
