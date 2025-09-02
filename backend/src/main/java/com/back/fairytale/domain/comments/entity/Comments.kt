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

    // 부모 댓글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val parent: Comments? = null,

) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    // 양방향 관계 : 자식 댓글
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val children: MutableList<Comments> = mutableListOf()

    init {
        validateHierarchy()
    }

    // 계층 구조 유효성 검사 (자식댓글은 자식댓글을 가질 수 없음)
    private fun validateHierarchy() {
        val parentComment = parent ?: return

        if (parentComment.parent != null) {
            throw IllegalArgumentException("대댓글에는 대댓글을 달 수 없습니다.")
        }
    }

    // 편의 메서드들
    fun isReply(): Boolean = parent != null // 대댓글 여부
    fun isParentComment(): Boolean = parent == null // 부모 댓글 여부
    fun hasChildren(): Boolean = children.isNotEmpty() // 자식 댓글 존재 여부
    fun getDepth(): Int = if (parent == null) 0 else 1 // 0: 부모 댓글, 1: 대댓글

    // Comments 수정
    fun updateContent(content: String) {
        this.content = content
    }
}
