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
    lateinit var createdAt: LocalDateTime
        private set // Java로 인식하여 인텔리제이에서 문법오류라고 표시할 가능성 있음

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    lateinit var updatedAt: LocalDateTime
        private set
}
