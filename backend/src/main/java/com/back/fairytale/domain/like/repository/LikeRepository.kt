package com.back.fairytale.domain.like.repository

import com.back.fairytale.domain.like.entity.Like
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface LikeRepository : JpaRepository<Like, Long> {

    fun findByUserId(userId: Long): List<Like>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Like l where l.user.id = :userId and l.fairytale.id = :fairytaleId")
    fun findByUserIdAndFairytaleId(userId: Long, fairytaleId: Long): Optional<Like>
}