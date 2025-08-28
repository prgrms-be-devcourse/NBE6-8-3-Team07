package com.back.fairytale.domain.like.repository

import com.back.fairytale.domain.like.entity.Like
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface LikeRepository : JpaRepository<Like, Long> {

    fun findByUserId(userId: Long): List<Like>

    fun findByUserIdAndFairytaleId(userId: Long, fairytaleId: Long): Optional<Like>
}