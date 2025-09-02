package com.back.fairytale.domain.user.entity

import com.back.fairytale.domain.bookmark.entity.BookMark
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.user.enums.IsDeleted
import com.back.fairytale.domain.user.enums.Role
import com.back.fairytale.global.entity.BaseEntity
import jakarta.persistence.*
import lombok.AccessLevel
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.NoArgsConstructor
import java.time.LocalDateTime


@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(length = 20, nullable = false)
    var name: String,

    @Column(length = 20, unique = true)
    var nickname: String,

    @Column(length = 50, nullable = false)
    var email: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private var isDeleted: IsDeleted = IsDeleted.NOT_DELETED,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER,

    @Column(unique = true, nullable = false)
    val socialId: String,

    @Column(unique = true, columnDefinition = "TEXT")
    var refreshToken: String? = null,

    // Grace Period를 위한 이전 리프레시 토큰 저장
    @Column(columnDefinition = "TEXT")
    var previousRefreshToken: String? = null,

    // 리프레시 토큰 업데이트 시간
    @Column
    var refreshTokenUpdatedAt: LocalDateTime? = null
) : BaseEntity() {

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val fairytales: MutableList<Fairytale> = mutableListOf()

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val favorites: MutableList<BookMark> = mutableListOf()

    fun update(name: String, nickname: String, email: String) = apply {
        this.name = name
        this.nickname = nickname
        this.email = email
    }

    fun delete() {
        this.isDeleted = IsDeleted.DELETED
    }
}