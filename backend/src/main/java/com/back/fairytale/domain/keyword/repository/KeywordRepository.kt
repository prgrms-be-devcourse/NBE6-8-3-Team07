package com.back.fairytale.domain.keyword.repository

import com.back.fairytale.domain.keyword.entity.Keyword
import com.back.fairytale.domain.keyword.enums.KeywordType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface KeywordRepository : JpaRepository<Keyword, Long> {
    fun findByKeyword(keyword: String): Optional<Keyword>
    fun findByKeywordType(keywordType: KeywordType): List<Keyword>
    fun findByKeywordAndKeywordType(keyword: String, keywordType: KeywordType): Optional<Keyword>
}
