package com.back.fairytale.domain.fairytale.entity

import com.back.fairytale.domain.keyword.entity.Keyword
import jakarta.persistence.*

@Entity
@Table(name = "fairytale_keyword")
class FairytaleKeyword(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fairytale_id", nullable = false)
    val fairytale: Fairytale,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    val keyword: Keyword
)
