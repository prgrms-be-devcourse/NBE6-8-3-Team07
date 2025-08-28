package com.back.fairytale.domain.thoughts.repository

import com.back.fairytale.domain.thoughts.entity.Thoughts
import org.springframework.data.jpa.repository.JpaRepository

interface ThoughtsRepository : JpaRepository<Thoughts, Long> {
    fun findByFairytaleIdAndUserId(fairytaleId: Long, userId: Long): Thoughts?
}
