package com.back.fairytale.domain.user.repository

import com.back.fairytale.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findBySocialId(socialId: String): User?
}
