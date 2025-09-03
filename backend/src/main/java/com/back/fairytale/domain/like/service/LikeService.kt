package com.back.fairytale.domain.like.service

import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.like.dto.LikeDto
import com.back.fairytale.domain.like.entity.Like
import com.back.fairytale.domain.like.exception.LikeAlreadyExistsException
import com.back.fairytale.domain.like.exception.LikeNotFoundException
import com.back.fairytale.domain.like.repository.LikeRepository
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.global.security.CustomOAuth2User
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Service
class LikeService(
    private val likeRepository: LikeRepository,
    private val userRepository: UserRepository,
    private val fairytaleRepository: FairytaleRepository,
    private val redissonClient: RedissonClient
) {

    @Transactional(readOnly = true)
    fun getLikes(user: CustomOAuth2User): List<LikeDto> {
        return likeRepository.findByUserId(user.id!!)
            .map { like -> LikeDto(like.fairytale.id!!) }
    }

    // 비관적 락
    @Transactional
    fun addLikePessimistic(userId: Long, fairytaleId: Long): Like {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("해당 유저를 찾을 수 없습니다.")

        val fairytale = fairytaleRepository.findByIdWithPessimisticLock(fairytaleId)
            ?: throw IllegalArgumentException("해당 동화를 찾을 수 없습니다.")

        if (likeRepository.findByUserIdAndFairytaleId(user.id!!, fairytale.id!!).isPresent) {
            throw LikeAlreadyExistsException("이미 좋아요를 누른 동화입니다.")
        }

        val like = Like.toEntity(user, fairytale)
        fairytale.increaseLikeCount()
        return likeRepository.saveAndFlush(like)
    }

    // 분산락
    @Transactional
    fun addLike(userId: Long, fairytaleId: Long): Like {
        val lockKey = "fairytale:like:$fairytaleId"
        val lock: RLock = redissonClient.getLock(lockKey)

        return try {
            if (lock.tryLock(5, 3, TimeUnit.SECONDS)) {
                val user = userRepository.findByIdOrNull(userId)
                    ?: throw IllegalArgumentException("해당 유저를 찾을 수 없습니다.")
                val fairytale = fairytaleRepository.findByIdOrNull(fairytaleId)
                    ?: throw IllegalArgumentException("해당 동화를 찾을 수 없습니다.")

                if (likeRepository.findByUserIdAndFairytaleId(user.id!!, fairytale.id!!).isPresent) {
                    throw LikeAlreadyExistsException("이미 좋아요를 누른 동화입니다.")
                }

                val like = Like.toEntity(user, fairytale)
                fairytale.increaseLikeCount()
                likeRepository.saveAndFlush(like)
            } else {
                throw IllegalStateException("락을 획득할 수 없습니다.")
            }
        } finally {
            if (lock.isHeldByCurrentThread) lock.unlock()
        }
    }

    @Transactional
    fun removeLike(userId: Long, fairytaleId: Long) {
        val lockKey = "fairytale:like:$fairytaleId"
        val lock: RLock = redissonClient.getLock(lockKey)

        try {
            if (lock.tryLock(5, 3, TimeUnit.SECONDS)) {
                val user = userRepository.findByIdOrNull(userId)
                    ?: throw IllegalArgumentException("해당 유저를 찾을 수 없습니다.")
                val fairytale = fairytaleRepository.findByIdOrNull(fairytaleId)
                    ?: throw IllegalArgumentException("해당 동화를 찾을 수 없습니다.")

                val like = likeRepository.findByUserIdAndFairytaleId(user.id!!, fairytale.id!!).orElse(null)
                    ?: throw LikeNotFoundException("좋아요가 없는 동화입니다.")

                fairytale.decreaseLikeCount()
                likeRepository.deleteById(like.id!!)
            } else {
                throw IllegalStateException("락을 획득할 수 없습니다. 다시 시도해주세요.")
            }
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

    @Transactional(readOnly = true)
    fun isLikedByUser(userId: Long, fairytaleId: Long): Boolean {
        return likeRepository.findByUserIdAndFairytaleId(userId, fairytaleId).isPresent
    }
}