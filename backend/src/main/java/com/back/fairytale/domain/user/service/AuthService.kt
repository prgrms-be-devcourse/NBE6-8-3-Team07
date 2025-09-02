package com.back.fairytale.domain.user.service

import com.back.fairytale.domain.refreshtoken.entity.RefreshToken
import com.back.fairytale.domain.refreshtoken.repository.RefreshTokenRepository
import com.back.fairytale.domain.user.dto.TokenPairDto
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.port.LogoutService
import com.back.fairytale.global.security.port.UserTokenService
import jakarta.servlet.http.Cookie
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional
class AuthService(
    private val jwtProvider: JWTProvider,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) : LogoutService, UserTokenService {


    fun getRefreshTokenFromCookies(cookies: Array<Cookie>?): String? {
        return jwtProvider.extractRefreshToken(cookies)
    }

    fun reissueTokens(refreshToken: String?): TokenPairDto {
        val user = validateRefreshTokenAndGetUser(refreshToken)

        val newAccessToken = jwtProvider.createAccessToken(user.id!!, user.role.key)
        val newRefreshToken = jwtProvider.createRefreshToken(user.id!!, user.role.key)

        saveOrUpdateUserToken(user.id!!, newRefreshToken)

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

        val refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken!!)
            ?: throw IllegalArgumentException("해당 유저가 존재하지 않습니다")

        return refreshTokenEntity.user
    }

    override fun saveOrUpdateUserToken(userId: Long, refreshToken: String) {
        val user = findUserById(userId)
        val userToken = RefreshToken(
            user = user,
            token = refreshToken,
        )
        refreshTokenRepository.save(userToken)
    }

    override fun logout(userId: Long) {
        refreshTokenRepository.deleteAllByUserId(userId)
    }

    private fun findUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("해당 유저가 존재하지 않습니다. $userId") }
    }

}