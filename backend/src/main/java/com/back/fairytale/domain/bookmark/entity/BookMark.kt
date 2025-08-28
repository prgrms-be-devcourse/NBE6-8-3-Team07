package com.back.fairytale.domain.bookmark.entity

import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.user.entity.User
import jakarta.persistence.*

@Entity
@Table(
    name = "bookmark",
    indexes = [
        Index(name = "idx_bookmark_user_fairytale", columnList = "user_id, fairytale_id")
    ]
)
class BookMark(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fairytale_id", nullable = false)
    val fairytale: Fairytale
) {
    companion object {
        fun toEntity(user: User, fairytale: Fairytale): BookMark {
            return BookMark(
                user = user,
                fairytale = fairytale
            )
        }
    }
}
