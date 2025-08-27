package com.back.fairytale.domain.fairytale.repository;

import com.back.fairytale.domain.fairytale.entity.Fairytale;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FairytaleRepository extends JpaRepository<Fairytale, Long> {

    List<Fairytale> findAllByUserIdOrderByCreatedAtDesc(Long userId);


    // Fetch Join으로 N+1 해결 - 상세 조회
    @Query("SELECT f FROM Fairytale f " +
            "LEFT JOIN FETCH f.fairytaleKeywords fk " +
            "LEFT JOIN FETCH fk.keyword " +
            "WHERE f.id = :fairytaleId AND f.user.id = :userId")
    Optional<Fairytale> findByIdAndUserIdWithKeywordsFetch(@Param("fairytaleId") Long fairytaleId,
                                                           @Param("userId") Long userId);


    // Fetch Join으로 N+1 해결 - 상세 조회 (공개용)
    @Query("SELECT f FROM Fairytale f " +
            "LEFT JOIN FETCH f.fairytaleKeywords fk " +
            "LEFT JOIN FETCH fk.keyword " +
            "WHERE f.id = :fairytaleId")
    Optional<Fairytale> findByIdWithKeywordsFetch(@Param("fairytaleId") Long fairytaleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fairytale f where f.id = :id")
    Optional<Fairytale> findByIdWithPessimisticLock(@Param("id") Long id);

    // 공개된 동화만 조회
    @Query("SELECT f FROM Fairytale f WHERE f.isPublic = true ORDER BY f.createdAt DESC")
    List<Fairytale> findAllPublicFairytales();

    // 특정 사용자의 공개 동화만 조회
    @Query("SELECT f FROM Fairytale f WHERE f.user.id = :userId AND f.isPublic = true ORDER BY f.createdAt DESC")
    List<Fairytale> findPublicFairytalesByUserId(@Param("userId") Long userId);

    // 갤러리용 공개 동화 조회 (페이징 지원 )
    @Query("SELECT f FROM Fairytale f WHERE f.isPublic = true ORDER BY f.createdAt DESC")
    Page<Fairytale> findPublicFairytalesForGallery(Pageable pageable);
}
