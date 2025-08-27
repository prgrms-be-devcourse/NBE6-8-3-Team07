package com.back.fairytale.domain.thoughts.entity

import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.thoughts.dto.ThoughtsRequest
import com.back.fairytale.domain.user.entity.User
import jakarta.persistence.*
import lombok.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "thoughts")
class Thoughts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fairytale_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private var fairytale: Fairytale? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private var user: User? = null

    // 아이 이름
    @Column(nullable = false, length = 50)
    private var name: String? = null

    // 아이 생각
    @Lob
    @Column(nullable = false)
    private var content: String? = null

    // 부모 생각
    @Lob
    @Column(nullable = false)
    private var parentContent: String? = null

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private var createdAt: LocalDateTime? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private var updatedAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        this.createdAt = LocalDateTime.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        this.updatedAt = LocalDateTime.now()
    }

    // Thoughts 수정
    fun update(name: String?, content: String?, parentContent: String?) {
        // 아이이름 수정
        if (name != null) {
            this.name = name.trim { it <= ' ' }
        }
        // 아이생각 수정
        if (content != null) {
            this.content = content.trim { it <= ' ' }
        }
        // 부모생각 수정
        if (parentContent != null) {
            this.parentContent = parentContent.trim { it <= ' ' }
        }
    }

    companion object {
        // Thoughts 생성
        @JvmStatic
        fun of(fairytale: Fairytale?, user: User?, request: ThoughtsRequest): Thoughts? {
            return Thoughts.builder()
                .fairytale(fairytale)
                .user(user)
                .name(request.name)
                .content(request.content)
                .parentContent(request.parentContent)
                .build()
        }
    }
}
