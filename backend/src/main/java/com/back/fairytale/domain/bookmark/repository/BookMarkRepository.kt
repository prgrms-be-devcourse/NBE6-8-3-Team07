package com.back.fairytale.domain.bookmark.repository

import com.back.fairytale.domain.bookmark.entity.BookMark
import org.springframework.data.jpa.repository.JpaRepository

interface BookMarkRepository : JpaRepository<BookMark, Long> {
    fun findByUserId(userId: Long): List<BookMark>
    fun findByUserIdAndFairytaleId(userId: Long, fairytaleId: Long): BookMark?
}
