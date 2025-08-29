package com.back.fairytale.domain.user.service

import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.enums.IsDeleted
import com.back.fairytale.domain.user.enums.Role
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.oauth2.OAuth2UserInfo
import com.back.fairytale.global.security.port.OAuth2UserManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OAuth2UserManagerImpl(
    private val userRepository: UserRepository,
    private val jwtProvider: JWTProvider
) : OAuth2UserManager {

    override fun saveOrUpdateUser(userInfo: MutableMap<String, Any>): OAuth2UserInfo {
        val socialId = userInfo["id"]?.toString() ?: throw IllegalArgumentException("Social ID is required")
        val name = userInfo["name"]?.toString() ?: throw IllegalArgumentException("Name is required")
        val nickname = userInfo["nickname"]?.toString() ?: throw IllegalArgumentException("Nickname is required")
        val email = userInfo["email"]?.toString() ?: throw IllegalArgumentException("Email is required")

        val user = userRepository.findBySocialId(socialId)?.update(name, nickname, email) ?: createUser(socialId, name, nickname, email)

        val savedUser = userRepository.save(user)
        val newRefreshToken = jwtProvider.createRefreshToken(
            userId = checkNotNull(savedUser.id) { "Saved user ID cannot be null" },
            role = savedUser.role.key
        )

        savedUser.refreshToken = newRefreshToken

        return OAuth2UserInfo(
            id = savedUser.id!!,
            socialId = savedUser.socialId,
            name = savedUser.name,
            nickname = savedUser.nickname,
            email = savedUser.email,
            role = savedUser.role.key
        )
    }

    private fun createUser(socialId: String, name: String, nickname: String, email: String): User {
        return User(
            socialId = socialId,
            name = name,
            nickname = nickname,
            email = email,
            role = Role.USER,
            isDeleted = IsDeleted.NOT_DELETED
        )
    }
}
