package com.back.fairytale.domain.like.service

import com.back.BackendApplication
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.like.entity.Like
import com.back.fairytale.domain.like.exception.LikeAlreadyExistsException
import com.back.fairytale.domain.like.exception.LikeNotFoundException
import com.back.fairytale.domain.like.repository.LikeRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.domain.user.service.AuthService
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.security.CustomOAuth2User
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.jwt.JWTUtil
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import com.google.cloud.storage.Storage
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@SpringBootTest(classes = [BackendApplication::class])
@ActiveProfiles("test")
class LikeServiceTest @Autowired constructor(
    private val likeService: LikeService,
    private val likeRepository: LikeRepository,
    private val userRepository: UserRepository,
    private val fairytaleRepository: FairytaleRepository
) {

    @MockkBean
    private lateinit var geminiClient: GeminiClient

    @MockkBean
    private lateinit var huggingFaceClient: HuggingFaceClient

    @MockkBean
    private lateinit var googleCloudStorage: GoogleCloudStorage

    @MockkBean
    private lateinit var storage: Storage

    @MockkBean
    private lateinit var authService: AuthService

    @MockkBean
    private lateinit var jwtProvider: JWTProvider

    @MockkBean
    private lateinit var jwtUtil: JWTUtil

    private lateinit var user: User
    private lateinit var fairytale: Fairytale

    @BeforeEach
    fun setUp() {
        likeRepository.deleteAll()
        fairytaleRepository.deleteAll()
        userRepository.deleteAll()

        user = User(
            email = "test@naver.com",
            name = "홍길동",
            nickname = "길동",
            socialId = "1234"
        )
        userRepository.save(user)

        fairytale = Fairytale(
            user = user,
            title = "안녕",
            content = "반가워요",
            imageUrl = "www.naver.com"
        )
        fairytaleRepository.save(fairytale)
    }

    @Test
    @Order(1)
    @DisplayName("비관적 락 - 1000이 동시에 좋아요 요청")
    fun concurrentLikesWithPessimisticLock_MultipleUsers() {
        val threadCount = 1000
        val executor = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(threadCount)

        // 1000명 유저 생성
        val users = (1..threadCount).map { i ->
            userRepository.save(
                User(
                    email = "user$i@test.com",
                    name = "User$i",
                    nickname = "nick$i",
                    socialId = "social$i"
                )
            )
        }

        val startTime = System.currentTimeMillis()

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    likeService.addLikePessimistic(users[i].id!!, fairytale.id!!)
                } catch (e: Exception) {
                    // 중복 예외 등 무시
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        val endTime = System.currentTimeMillis()

        println("=========================================")
        println("총 소요 시간: ${endTime - startTime} ms")
        println("=========================================")

        assertEquals(threadCount, likeRepository.count().toInt())
    }

    @Test
    @Order(2)
    @DisplayName("Redis 분산락 - 1000명이 동시에 좋아요 요청")
    fun concurrentLikesWithRedisLock_MultipleUsers() {
        val threadCount = 1000
        val executor = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(threadCount)

        // 1000명 유저 생성
        val users = (1..threadCount).map { i ->
            userRepository.save(
                User(
                    email = "user$i@test.com",
                    name = "User$i",
                    nickname = "nick$i",
                    socialId = "social$i"
                )
            )
        }

        val startTime = System.currentTimeMillis()

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    likeService.addLike(users[i].id!!, fairytale.id!!)
                } catch (e: Exception) {
                    // 중복 예외 등 무시
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        val endTime = System.currentTimeMillis()

        println("=========================================")
        println("총 소요 시간: ${endTime - startTime} ms")
        println("=========================================")

        assertEquals(threadCount, likeRepository.count().toInt())
    }

    @Test
    @DisplayName("좋아요 목록 조회")
    fun getLikes() {
        val like = Like(user = user, fairytale = fairytale)
        likeRepository.save(like)

        val result = likeService.getLikes(CustomOAuth2User(user.id!!, user.name, "ROLE_USER"))

        assertEquals(1, result.size)
        assertEquals(fairytale.id, result[0].fairytaleId)
    }

    @Test
    @DisplayName("좋아요 추가")
    fun addLike() {
        val result = likeService.addLike(user.id!!, fairytale.id!!)

        assertNotNull(result.id)
        assertEquals(user.id, result.user.id)
        assertEquals(fairytale.id, result.fairytale.id)
    }

    @Test
    @DisplayName("중복 좋아요 추가 불가")
    fun preventDuplicateLike() {
        likeService.addLike(user.id!!, fairytale.id!!)

        assertThrows(LikeAlreadyExistsException::class.java) {
            likeService.addLike(user.id!!, fairytale.id!!)
        }
    }

    @Test
    @DisplayName("좋아요 삭제")
    fun removeLike() {
        val like = likeService.addLike(user.id!!, fairytale.id!!)

        likeService.removeLike(user.id!!, fairytale.id!!)

        assertFalse(likeRepository.findById(like.id!!).isPresent)
    }

    @Test
    @DisplayName("좋아요되지 않은 동화를 삭제하면 예외 발생")
    fun removeNonExistentLike() {
        assertThrows(LikeNotFoundException::class.java) {
            likeService.removeLike(user.id!!, fairytale.id!!)
        }
    }

    @Test
    @DisplayName("좋아요 여부 확인 - true")
    fun checkLikeExists() {
        likeService.addLike(user.id!!, fairytale.id!!)

        val result = likeService.isLikedByUser(user.id!!, fairytale.id!!)

        assertTrue(result)
    }

    @Test
    @DisplayName("좋아요 여부 확인 - false")
    fun checkLikeNotExists() {
        val result = likeService.isLikedByUser(user.id!!, fairytale.id!!)

        assertFalse(result)
    }
}