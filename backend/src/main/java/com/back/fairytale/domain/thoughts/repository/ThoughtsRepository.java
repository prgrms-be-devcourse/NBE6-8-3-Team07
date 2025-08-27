package com.back.fairytale.domain.thoughts.repository;

import com.back.fairytale.domain.thoughts.entity.Thoughts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThoughtsRepository extends JpaRepository<Thoughts, Long> {
    Optional<Thoughts> findByFairytaleIdAndUserId(Long fairytaleId, Long userId);
}
