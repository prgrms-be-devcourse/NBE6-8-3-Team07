package com.back.fairytale.global.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Column(name = "created_at", updatable = false, nullable = false)
    @CreatedDate
    var createdAt: LocalDateTime? = null
        protected set // Kotlin과 IDE와의 충돌로 문법오류로 인식, 추후 테스트 후 수정필요할시 수정 예정

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null
        protected set
}
