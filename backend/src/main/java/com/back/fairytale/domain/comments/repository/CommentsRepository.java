package com.back.fairytale.domain.comments.repository;

import com.back.fairytale.domain.comments.entity.Comments;
import com.back.fairytale.domain.fairytale.entity.Fairytale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentsRepository extends JpaRepository<Comments, Long> {
    // 특정 fairytale에 대한 comments를 조회하는 method
    Page<Comments> findByFairytale(Fairytale fairytale, Pageable pageable);

}
