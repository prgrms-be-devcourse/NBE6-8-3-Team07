package com.back.fairytale.domain.keyword.repository;

import com.back.fairytale.domain.keyword.entity.Keyword;
import com.back.fairytale.domain.keyword.enums.KeywordType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    Optional<Keyword> findByKeyword(String keyword);
    List<Keyword> findByKeywordType(KeywordType keywordType);
    Optional<Keyword> findByKeywordAndKeywordType(String keyword, KeywordType keywordType);
}