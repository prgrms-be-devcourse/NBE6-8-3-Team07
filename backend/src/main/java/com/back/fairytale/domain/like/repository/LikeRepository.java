package com.back.fairytale.domain.like.repository;

import com.back.fairytale.domain.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    List<Like> findByUserId(Long userId);

    Optional<Like> findByUserIdAndFairytaleId(Long userId, Long fairytaleId);
}
