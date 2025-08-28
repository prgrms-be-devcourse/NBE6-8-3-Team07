package com.back.fairytale.domain.fairytale.entity

import com.back.fairytale.domain.bookmark.entity.BookMark
import com.back.fairytale.domain.keyword.entity.Keyword
import com.back.fairytale.domain.keyword.enums.KeywordType
import com.back.fairytale.domain.thoughts.entity.Thoughts
import com.back.fairytale.domain.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "fairytale")
class Fairytale(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 200)
    var title: String,

    @Lob
    @Column(nullable = false)
    var content: String,

    @Column(name = "image_url", length = 255)
    var imageUrl: String? = null,

    @Column
    var likeCount: Long? = 0L,

    @Column(name = "is_public", nullable = false)
    var isPublic: Boolean = false

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @OneToMany(mappedBy = "fairytale", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val fairytaleKeywords: MutableList<FairytaleKeyword> = mutableListOf()

    @OneToMany(mappedBy = "fairytale", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookmarks: MutableList<BookMark> = mutableListOf()

    @OneToMany(mappedBy = "fairytale", cascade = [CascadeType.ALL], orphanRemoval = true)
    val thoughts: MutableList<Thoughts> = mutableListOf()

    // === 비즈니스 메서드 ===
    fun addKeyword(keyword: Keyword) {
        val fairytaleKeyword = FairytaleKeyword(
            fairytale = this,
            keyword = keyword
        )
        this.fairytaleKeywords.add(fairytaleKeyword)
    }

    fun getChildName(): String? = getFirstKeywordByType(KeywordType.CHILD_NAME)

    fun getChildRole(): String? = getFirstKeywordByType(KeywordType.CHILD_ROLE)

    fun getCharacters(): String? = getJoinedKeywordsByType(KeywordType.CHARACTERS)

    fun getPlace(): String? = getJoinedKeywordsByType(KeywordType.PLACE)

    fun getLesson(): String? = getJoinedKeywordsByType(KeywordType.LESSON)

    fun getMood(): String? = getJoinedKeywordsByType(KeywordType.MOOD)

    private fun getFirstKeywordByType(type: KeywordType): String? =
        getKeywordsByType(type).firstOrNull()?.keyword?.keyword

    private fun getJoinedKeywordsByType(type: KeywordType): String? {
        val keywords = getKeywordsByType(type).map { it.keyword.keyword }
        return if (keywords.isEmpty()) null else keywords.joinToString(", ")
    }

    private fun getKeywordsByType(type: KeywordType): List<FairytaleKeyword> =
        fairytaleKeywords.filter { it.keyword.keywordType == type }

    fun increaseLikeCount() {
        this.likeCount = (this.likeCount ?: 0L) + 1
    }

    fun decreaseLikeCount() {
        this.likeCount = (this.likeCount ?: 0L).coerceAtLeast(1L) - 1
    }

}