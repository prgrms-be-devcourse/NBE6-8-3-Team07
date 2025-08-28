package com.back.fairytale.domain.comments.entity

import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.global.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "comments")
class Comments (
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fairytale_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val fairytale: Fairytale,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    // 댓글 내용
    @Column(length = 500, nullable = false)
    var content: String,

) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    // Comments 수정
    fun updateContent(content: String) {
        this.content = content
    }
}
