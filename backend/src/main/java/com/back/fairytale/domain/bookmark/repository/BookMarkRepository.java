package com.back.fairytale.domain.bookmark.repository;

import com.back.fairytale.domain.bookmark.entity.BookMark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookMarkRepository extends JpaRepository<BookMark, Long> {

    List<BookMark> findByUserId(Long userId);

    Optional<BookMark> findByUserIdAndFairytaleId(Long userId, Long fairytaleId);
}
