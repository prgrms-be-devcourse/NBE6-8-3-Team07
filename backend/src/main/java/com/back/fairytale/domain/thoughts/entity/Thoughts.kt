package com.back.fairytale.domain.thoughts.entity

import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.thoughts.dto.ThoughtsRequest
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.global.entity.BaseEntity
import jakarta.persistence.*
import lombok.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "thoughts")
class Thoughts (

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fairytale_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val fairytale: Fairytale,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    // 아이 이름
    @Column(nullable = false, length = 50)
    var name: String,

    // 아이 생각
    @Lob
    @Column(nullable = false)
    var content: String,

    // 부모 생각
    @Lob
    @Column(nullable = false)
    var parentContent: String,

) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    // Thoughts 수정
    fun update(name: String?, content: String?, parentContent: String?) {
        // 아이이름 수정
        name?.let { this.name = it.trim() }
        // 아이생각 수정
        content?.let { this.content = it.trim() }
        // 부모생각 수정
        parentContent?.let { this.parentContent =  it.trim() }
    }
}
