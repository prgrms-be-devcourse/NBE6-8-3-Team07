package com.back.fairytale.domain.fairytale.dto

import com.back.fairytale.domain.fairytale.entity.Fairytale
import java.time.LocalDateTime

data class FairytaleDetailResponse(
    val id: Long,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val isPublic: Boolean,
    val childName: String,
    val childRole: String,
    val characters: String,
    val place: String,
    val lesson: String,
    val mood: String,
    val createdAt: LocalDateTime
) {
    constructor(fairytale: Fairytale) : this(
        id = fairytale.id!!,
        title = fairytale.title,
        content = fairytale.content,
        imageUrl = fairytale.imageUrl,
        isPublic = fairytale.isPublic,
        childName = fairytale.getChildName()!!,
        childRole = fairytale.getChildRole()!!,
        characters = fairytale.getCharacters()!!,
        place = fairytale.getPlace()!!,
        lesson = fairytale.getLesson()!!,
        mood = fairytale.getMood()!!,
        createdAt = fairytale.createdAt!!
    )
}