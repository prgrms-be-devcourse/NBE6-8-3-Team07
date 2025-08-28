package com.back.fairytale.domain.fairytale.dto

import com.back.fairytale.domain.fairytale.entity.Fairytale
import java.time.LocalDate

data class FairytalePublicListResponse(
    val id: Long,
    val title: String,
    val childName: String,
    val childRole: String,
    val characters: String,
    val place: String,
    val mood: String,
    val lesson: String,
    val createdAt: LocalDate,
    val likeCount: Long
) {
    constructor(fairytale: Fairytale) : this(
        id = fairytale.id!!,
        title = fairytale.title,
        childName = fairytale.getChildName()!!,
        childRole = fairytale.getChildRole()!!,
        characters = removeDuplicates(fairytale.getCharacters()!!),
        place = removeDuplicates(fairytale.getPlace()!!),
        mood = removeDuplicates(fairytale.getMood()!!),
        lesson = removeDuplicates(fairytale.getLesson()!!),
        createdAt = fairytale.createdAt!!.toLocalDate(),
        likeCount = fairytale.likeCount ?: 0L
    )

    companion object {
        private fun removeDuplicates(keywords: String): String =
            keywords.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
    }
}