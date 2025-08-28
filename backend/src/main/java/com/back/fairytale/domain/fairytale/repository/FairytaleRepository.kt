package com.back.fairytale.domain.fairytale.repository

import com.back.fairytale.domain.fairytale.entity.Fairytale
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FairytaleRepository : JpaRepository<Fairytale, Long> {

    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Fairytale>

    // Fetch Join으로 N+1 해결 - 상세 조회
    @Query(
        """
        SELECT f FROM Fairytale f
        LEFT JOIN FETCH f.fairytaleKeywords fk
        LEFT JOIN FETCH fk.keyword
        WHERE f.id = :fairytaleId AND f.user.id = :userId
        """
    )
    fun findByIdAndUserIdWithKeywordsFetch(
        @Param("fairytaleId") fairytaleId: Long,
        @Param("userId") userId: Long
    ): Fairytale?

    // Fetch Join으로 N+1 해결 - 상세 조회 (공개용)
    @Query(
        """
        SELECT f FROM Fairytale f
        LEFT JOIN FETCH f.fairytaleKeywords fk
        LEFT JOIN FETCH fk.keyword
        WHERE f.id = :fairytaleId
        """
    )
    fun findByIdWithKeywordsFetch(
        @Param("fairytaleId") fairytaleId: Long
    ): Fairytale?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Fairytale f WHERE f.id = :id")
    fun findByIdWithPessimisticLock(@Param("id") id: Long): Fairytale?

    // 공개된 동화만 조회
    @Query("SELECT f FROM Fairytale f WHERE f.isPublic = true ORDER BY f.createdAt DESC")
    fun findAllPublicFairytales(): List<Fairytale>

    // 특정 사용자의 공개 동화만 조회
    @Query("SELECT f FROM Fairytale f WHERE f.user.id = :userId AND f.isPublic = true ORDER BY f.createdAt DESC")
    fun findPublicFairytalesByUserId(@Param("userId") userId: Long): List<Fairytale>

    // 갤러리용 공개 동화 조회 (페이징 지원)
    @Query("SELECT f FROM Fairytale f WHERE f.isPublic = true ORDER BY f.createdAt DESC")
    fun findPublicFairytalesForGallery(pageable: Pageable): Page<Fairytale>
}
