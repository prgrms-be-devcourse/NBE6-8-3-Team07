package com.back.fairytale.global.lock

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class LockService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    fun executeWithLock(key: String, timeout: Long = 3000L, action: () -> Any): Any {
        val lockKey = "lock:$key"
        val lockValue = Thread.currentThread().id.toString()

        if (!acquireLock(lockKey, lockValue, timeout)) {
            throw RuntimeException("락 획득 실패")
        }

        try {
            return action()
        } finally {
            releaseLock(lockKey, lockValue)
        }
    }

    private fun acquireLock(lockKey: String, lockValue: String, timeout: Long): Boolean {
        val acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofMillis(timeout))
        return acquired != null && acquired
    }

    private fun releaseLock(lockKey: String, lockValue: String) {
        val currentValue = redisTemplate.opsForValue().get(lockKey)
        if (currentValue == lockValue) {
            redisTemplate.delete(lockKey)
        }
    }
}