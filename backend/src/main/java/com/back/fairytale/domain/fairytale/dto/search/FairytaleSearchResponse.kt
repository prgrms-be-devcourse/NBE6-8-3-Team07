package com.back.fairytale.domain.fairytale.dto.search

import com.back.fairytale.domain.fairytale.entity.Fairytale
import java.time.LocalDateTime

data class FairytaleSearchResponse (
    val id: Long,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val childName: String?,
    val childRole: String?,
    val characters: String?,
    val place: String?,
    val lesson: String?,
    val mood: String?,
    val userId: Long,
    val createdAt: LocalDateTime,
    ) {
    companion object {
        @JvmStatic
        fun from(fairytale: Fairytale): FairytaleSearchResponse {
            return FairytaleSearchResponse(
                id = fairytale.id ?: throw IllegalArgumentException("동화 Id는 필수입니다."),
                title = fairytale.title,
                content = fairytale.content,
                imageUrl = fairytale.imageUrl,
                childName = fairytale.getChildName(),
                childRole = fairytale.getChildRole(),
                characters = fairytale.getCharacters(),
                place = fairytale.getPlace(),
                lesson = fairytale.getLesson(),
                mood = fairytale.getMood(),
                userId = fairytale.user.id ?: throw IllegalArgumentException("사용자 Id는 필수입니다."),
                createdAt = fairytale.createdAt ?: LocalDateTime.now(),
            )
        }
    }
}