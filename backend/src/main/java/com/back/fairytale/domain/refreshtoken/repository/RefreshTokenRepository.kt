package com.back.fairytale.domain.refreshtoken.repository

import com.back.fairytale.domain.refreshtoken.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?

    fun findAllByUserId(userId: Long): List<RefreshToken>

    @Transactional(readOnly = true)
    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.user WHERE rt.token = :token")
    fun findByTokenWithUser(@Param("token") token: String): RefreshToken?

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    fun deleteAllByUserId(@Param("userId") userId: Long): Int
}