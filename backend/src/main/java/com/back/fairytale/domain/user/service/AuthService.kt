package com.back.fairytale.domain.user.service

import com.back.fairytale.domain.user.dto.TokenPairDto
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.port.LogoutService
import com.back.fairytale.global.security.port.UserTokenService
import jakarta.servlet.http.Cookie
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.function.Supplier


@Service
@Transactional
class AuthService(
    private val jwtProvider: JWTProvider,
    private val userRepository: UserRepository
) : LogoutService, UserTokenService {


    fun getRefreshTokenFromCookies(cookies: Array<Cookie>?): String? {
        return jwtProvider.extractRefreshToken(cookies)
    }

    fun reissueTokens(refreshToken: String?): TokenPairDto {
        val user = validateRefreshTokenAndGetUser(refreshToken)

        val newAccessToken = jwtProvider.createAccessToken(user.id!!, user.role.key)
        val newRefreshToken = jwtProvider.createRefreshToken(user.id!!, user.role.key)

        user.refreshToken = newRefreshToken

        return TokenPairDto(newAccessToken, newRefreshToken)
    }

    fun createAccessTokenCookie(token: String): Cookie {
        return jwtProvider.wrapAccessTokenToCookie(token)
    }

    fun createRefreshTokenCookie(refreshToken: String): Cookie {
        return jwtProvider.wrapRefreshTokenToCookie(refreshToken)
    }

    private fun validateRefreshTokenAndGetUser(refreshToken: String?): User {
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw IllegalArgumentException("Refresh token이 유효하지 않습니다.")
        }

        val userId = jwtProvider.getUserIdFromRefreshToken(refreshToken)
        val user = findUserById(userId)

        if (refreshToken != user.refreshToken) {
            throw IllegalArgumentException("Refresh Token이 일치하지 않습니다.")
        }

        return user
    }

    override fun logout(userId: Long) {
        val findUser = findUserById(userId)
        findUser.refreshToken = null
    }

    override fun getUserToken(userId: Long): String {
        return findUserById(userId).refreshToken!!
    }

    private fun findUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("해당 유저가 존재하지 않습니다. $userId") }
    }

}