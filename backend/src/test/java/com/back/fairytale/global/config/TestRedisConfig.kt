package com.back.fairytale.global.config

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.redisson.spring.data.connection.RedissonConnectionFactory
import redis.embedded.RedisServer
import java.io.IOException
import java.net.ServerSocket

@Configuration
@Profile("test")
class TestRedisConfig {

    private val log = LoggerFactory.getLogger(TestRedisConfig::class.java)
    private var redisServer: RedisServer? = null
    private var redisPort: Int = 0

    @PostConstruct
    fun startRedis() {
        redisPort = findAvailablePort()
        try {
            redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("maxheap 128M")
                .build()
            redisServer?.start()
            log.info("내장 Redis 서버가 포트 {}에서 시작되었습니다.", redisPort)
            // 동적으로 할당된 포트를 시스템 프로퍼티에 설정
            System.setProperty("spring.data.redis.port", redisPort.toString())
        } catch (e: IOException) {
            log.error("내장 Redis 서버를 시작하는 중 오류 발생", e)
            throw RuntimeException(e)
        }
    }

    @PreDestroy
    fun stopRedis() {
        redisServer?.let { server ->
            if (server.isActive) {
                server.stop()
                log.info("내장 Redis 서버가 종료되었습니다.")
            }
        }
    }

    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()
        val host = "127.0.0.1"
        config.useSingleServer().setAddress("redis://$host:$redisPort")
        return Redisson.create(config)
    }

    @Bean
    fun redisConnectionFactory(redissonClient: RedissonClient): RedissonConnectionFactory {
        return RedissonConnectionFactory(redissonClient)
    }

    private fun findAvailablePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }
}
