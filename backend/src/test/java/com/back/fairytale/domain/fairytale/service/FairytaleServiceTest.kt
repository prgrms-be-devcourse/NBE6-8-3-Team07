package com.back.fairytale.domain.fairytale.service

import com.back.fairytale.domain.fairytale.dto.FairytaleCreateRequest
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.exception.FairytaleNotFoundException
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.keyword.entity.Keyword
import com.back.fairytale.domain.keyword.enums.KeywordType
import com.back.fairytale.domain.keyword.repository.KeywordRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import com.google.cloud.storage.Storage
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FairytaleServiceTest {

    @Autowired lateinit var fairytaleService: FairytaleService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var fairytaleRepository: FairytaleRepository
    @Autowired lateinit var keywordRepository: KeywordRepository

    // 외부 의존은 Mock 으로 대체 (서비스 빈 주입용)
    @MockkBean lateinit var geminiClient: GeminiClient
    @MockkBean lateinit var huggingFaceClient: HuggingFaceClient
    @MockkBean(relaxed = true) lateinit var googleCloudStorage: GoogleCloudStorage
    @MockkBean lateinit var storage: Storage

    private var savedUserId: Long = -1

    @BeforeEach
    fun setUpUser() {
        val saved = userRepository.save(
            User(
                name = "tester",
                nickname = "nick",
                email = "tester@example.com",
                socialId = "social"
            )
        )
        savedUserId = saved.id!!
    }

    @Test
    @DisplayName("getAllFairytalesByUserId: 동화 없음")
    fun t1() {
        assertThatThrownBy {
            fairytaleService.getAllFairytalesByUserId(savedUserId)
        }
            .isInstanceOf(FairytaleNotFoundException::class.java)
            .hasMessageContaining("등록된 동화가 없습니다.")
    }

    @Test
    @DisplayName("getAllFairytalesByUserId: 2건 저장 후 조회")
    fun t2() {
        val user = userRepository.findById(savedUserId).orElseThrow()
        val older = fairytaleRepository.save(
            com.back.fairytale.domain.fairytale.entity.Fairytale(
                user = user,
                title = "첫 번째 동화",
                content = "내용1",
                imageUrl = null,
                isPublic = false
            )
        )
        Thread.sleep(5) // createdAt 차이 만들려고

        val newer = fairytaleRepository.save(
            com.back.fairytale.domain.fairytale.entity.Fairytale(
                user = user,
                title = "두 번째 동화",
                content = "내용2",
                imageUrl = "https://example.com/img.jpg",
                isPublic = true
            )
        )

        val list = fairytaleService.getAllFairytalesByUserId(savedUserId)

        assertThat(list).hasSize(2)

        assertThat(list[0].id).isEqualTo(newer.id)
        assertThat(list[0].title).isEqualTo("두 번째 동화")
        assertThat(list[0].imageUrl).isEqualTo("https://example.com/img.jpg")
        assertThat(list[0].isPublic).isTrue
        assertThat(list[0].createdAt).isEqualTo(newer.createdAt!!.toLocalDate())

        assertThat(list[1].id).isEqualTo(older.id)
        assertThat(list[1].title).isEqualTo("첫 번째 동화")
        assertThat(list[1].imageUrl).isNull()
        assertThat(list[1].isPublic).isFalse
        assertThat(list[1].createdAt).isEqualTo(older.createdAt!!.toLocalDate())
    }

    @Test
    @DisplayName("getFairytaleByIdAndUserId: 상세 조회 성공")
    fun t3() {
        val user = userRepository.findById(savedUserId).orElseThrow()

        // 동화 저장
        val fairytale = fairytaleRepository.save(
            Fairytale(
                user = user,
                title = "용감한 기사 철수의 모험",
                content = "옛날 옛적에...",
                imageUrl = "https://example.com/image.jpg",
                isPublic = true
            )
        )

        // 키워드 저장 후 동화에 연결
        val kChildName = keywordRepository.save(Keyword.of("철수", KeywordType.CHILD_NAME))
        val kChildRole = keywordRepository.save(Keyword.of("용감한 기사", KeywordType.CHILD_ROLE))
        val kCharacters = keywordRepository.save(Keyword.of("마법사, 공주, 드래곤", KeywordType.CHARACTERS))
        val kPlace = keywordRepository.save(Keyword.of("신비한 숲, 마법의 성", KeywordType.PLACE))
        val kLesson = keywordRepository.save(Keyword.of("용기, 우정", KeywordType.LESSON))
        val kMood = keywordRepository.save(Keyword.of("모험적인, 따뜻한", KeywordType.MOOD))

        fairytale.addKeyword(kChildName)
        fairytale.addKeyword(kChildRole)
        fairytale.addKeyword(kCharacters)
        fairytale.addKeyword(kPlace)
        fairytale.addKeyword(kLesson)
        fairytale.addKeyword(kMood)
        // 연관관계 변경 반영
        fairytaleRepository.saveAndFlush(fairytale)

        val detail = fairytaleService.getFairytaleByIdAndUserId(fairytale.id!!, savedUserId)

        assertThat(detail.id).isEqualTo(fairytale.id)
        assertThat(detail.title).isEqualTo("용감한 기사 철수의 모험")
        assertThat(detail.content).isEqualTo("옛날 옛적에...")
        assertThat(detail.imageUrl).isEqualTo("https://example.com/image.jpg")
        assertThat(detail.isPublic).isTrue()

        assertThat(detail.childName).isEqualTo("철수")
        assertThat(detail.childRole).isEqualTo("용감한 기사")
        assertThat(detail.characters).isEqualTo("마법사, 공주, 드래곤")
        assertThat(detail.place).isEqualTo("신비한 숲, 마법의 성")
        assertThat(detail.lesson).isEqualTo("용기, 우정")
        assertThat(detail.mood).isEqualTo("모험적인, 따뜻한")
    }

    @Test
    @DisplayName("getFairytaleByIdAndUserId: 동화 없음")
    fun t4() {
        val notExistId = 999999L

        assertThatThrownBy {
            fairytaleService.getFairytaleByIdAndUserId(notExistId, savedUserId)
        }
            .isInstanceOf(FairytaleNotFoundException::class.java)
            .hasMessageContaining("동화를 찾을 수 없거나 접근 권한이 없습니다")
            .hasMessageContaining(notExistId.toString())
    }

    @Test
    @DisplayName("deleteFairytaleByIdAndUserId: 동화 삭제 성공")
    fun t5() {
        val user = userRepository.findById(savedUserId).orElseThrow()
        val imageUrl = "https://storage.googleapis.com/dummy-bucket/f1.png"

        val fairytale = fairytaleRepository.save(
            Fairytale(
                user = user,
                title = "지울 동화",
                content = "내용",
                imageUrl = imageUrl,   // 이미지가 있으므로 GCS 삭제도 호출되어야 함
                isPublic = false
            )
        )

        fairytaleService.deleteFairytaleByIdAndUserId(fairytale.id!!, savedUserId)

        assertThat(fairytaleRepository.findById(fairytale.id!!)).isEmpty
        verify(exactly = 1) { googleCloudStorage.deleteImages(listOf(imageUrl)) }
    }

    @Test
    @DisplayName("createFairytale: 동화 생성 성공")
    fun t6() {
        val req = FairytaleCreateRequest(
            childName = "철수",
            childRole = "용감한 기사",
            characters = "마법사, 공주, 드래곤",
            place = "신비한 숲",
            lesson = "용기, 우정",
            mood = "모험적인"
        )

        // Gemini(동화 본문 생성) 스텁
        every {
            geminiClient.generateFairytale(match { it.startsWith("어린이를 위한 동화를") })
        } returns "[제목: 용감한 기사 철수의 모험]\n옛날 옛적에 용감한 기사 철수가 살았습니다..."

        // Gemini(이미지 프롬프트 번역) 스텁
        every {
            geminiClient.generateFairytale(match { it.startsWith("다음 한국어 문장을 영어로 번역") })
        } returns "A smiling brave knight with a wizard, a princess, and a dragon in a magical forest..."

        // 이미지 생성/업로드 스텁
        val fakeImage = "img".toByteArray()
        every { huggingFaceClient.generateImage(any()) } returns fakeImage
        val uploadedUrl = "https://storage.googleapis.com/dummy-bucket/gen.png"
        every { googleCloudStorage.uploadImageBytesToCloud(fakeImage) } returns uploadedUrl

        val res = fairytaleService.createFairytale(req, savedUserId)

        assertThat(res.id).isNotNull()
        assertThat(res.title).isEqualTo("용감한 기사 철수의 모험")
        assertThat(res.content).contains("옛날 옛적에")
        assertThat(res.imageUrl).isEqualTo(uploadedUrl)
        assertThat(res.userId).isEqualTo(savedUserId)
        assertThat(res.createdAt).isNotNull()

        // DB 반영 검증
        val all = fairytaleRepository.findAllByUserIdOrderByCreatedAtDesc(savedUserId)
        assertThat(all).hasSize(1)

        // 키워드 저장/연결 검증(존재 여부만 체크)
        assertThat(keywordRepository.findByKeywordAndKeywordType("철수", KeywordType.CHILD_NAME).isPresent).isTrue()
        assertThat(keywordRepository.findByKeywordAndKeywordType("용감한 기사", KeywordType.CHILD_ROLE).isPresent).isTrue()
        assertThat(keywordRepository.findByKeywordAndKeywordType("마법사", KeywordType.CHARACTERS).isPresent).isTrue()
        assertThat(keywordRepository.findByKeywordAndKeywordType("공주", KeywordType.CHARACTERS).isPresent).isTrue()
        assertThat(keywordRepository.findByKeywordAndKeywordType("드래곤", KeywordType.CHARACTERS).isPresent).isTrue()
        assertThat(keywordRepository.findByKeywordAndKeywordType("신비한 숲", KeywordType.PLACE).isPresent).isTrue()
        assertThat(keywordRepository.findByKeywordAndKeywordType("용기", KeywordType.LESSON).isPresent).isTrue()
        assertThat(keywordRepository.findByKeywordAndKeywordType("우정", KeywordType.LESSON).isPresent).isTrue()
        assertThat(keywordRepository.findByKeywordAndKeywordType("모험적인", KeywordType.MOOD).isPresent).isTrue()

        // 외부 호출 횟수 검증
        verify(exactly = 1) { geminiClient.generateFairytale(match { it.startsWith("어린이를 위한 동화를") }) }
        verify(exactly = 1) { geminiClient.generateFairytale(match { it.startsWith("다음 한국어 문장을 영어로 번역") }) }
        verify(exactly = 1) { huggingFaceClient.generateImage(any()) }
        verify(exactly = 1) { googleCloudStorage.uploadImageBytesToCloud(fakeImage) }
    }

    @Test
    @DisplayName("createFairytale: 이미지 생성 실패해도 동화는 저장")
    fun t7() {
        val req = FairytaleCreateRequest(
            childName = "철수",
            childRole = "용감한 기사",
            characters = "마법사, 공주, 드래곤",
            place = "신비한 숲",
            lesson = "용기, 우정",
            mood = "모험적인"
        )

        every {
            geminiClient.generateFairytale(match { it.startsWith("어린이를 위한 동화를") })
        } returns "[제목: 용감한 기사 철수의 모험]\n옛날 옛적에..."

        every {
            geminiClient.generateFairytale(match { it.startsWith("다음 한국어 문장을 영어로 번역") })
        } returns "translated image prompt"

        // 이미지 생성 실패 유도
        every { huggingFaceClient.generateImage(any()) } throws RuntimeException("HF down")

        val res = fairytaleService.createFairytale(req, savedUserId)

        // 성공적으로 동화는 저장, imageUrl은 null
        assertThat(res.id).isNotNull()
        assertThat(res.title).isEqualTo("용감한 기사 철수의 모험")
        assertThat(res.imageUrl).isNull()

        // DB에도 1건 저장되었는지 확인
        val all = fairytaleRepository.findAllByUserIdOrderByCreatedAtDesc(savedUserId)
        assertThat(all).hasSize(1)
        assertThat(all[0].imageUrl).isNull()

        // 이미지 생성 실패였으므로 업로드는 호출되지 않아야 함
        verify(exactly = 0) { googleCloudStorage.uploadImageBytesToCloud(any()) }
    }
}
