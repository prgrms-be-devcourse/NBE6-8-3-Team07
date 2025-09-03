package com.back.fairytale.global.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("!test") // test 환경에서는 이 설정이 로드되지않고 내장 Redis 사용
@Configuration
class RedissonConfig {

    @Bean
    fun redissonClient(): RedissonClient {
        return Redisson.create(
            Config().apply {
                useSingleServer().address = "redis://127.0.0.1:6379"
            }
        )
    }
}