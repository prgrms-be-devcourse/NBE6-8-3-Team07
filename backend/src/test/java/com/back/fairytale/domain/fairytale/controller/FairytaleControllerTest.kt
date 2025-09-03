package com.back.fairytale.domain.fairytale.controller

import com.back.fairytale.domain.fairytale.dto.FairytaleCreateRequest
import com.back.fairytale.domain.fairytale.dto.FairytaleDetailResponse
import com.back.fairytale.domain.fairytale.dto.FairytaleResponse
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.keyword.entity.Keyword
import com.back.fairytale.domain.keyword.enums.KeywordType
import com.back.fairytale.domain.keyword.repository.KeywordRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.security.CustomOAuth2User
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.storage.Storage
import com.ninjasquad.springmockk.MockkBean
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class FairytaleControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var fairytaleRepository: FairytaleRepository

    @Autowired
    lateinit var keywordRepository: KeywordRepository

    @MockkBean lateinit var geminiClient: GeminiClient
    @MockkBean lateinit var huggingFaceClient: HuggingFaceClient
    @MockkBean(relaxed = true) lateinit var googleCloudStorage: GoogleCloudStorage
    @MockkBean lateinit var storage: Storage

    private var savedUserId: Long = -1L

    @BeforeEach
    fun setUpUser() {
        savedUserId = userRepository.save(
            User(
                name = "tester",
                nickname = "nick",
                email = "tester@example.com",
                socialId = "social"
            )
        ).id!!

        // SecurityContext에 인증 객체 직접 세팅
        val principal = CustomOAuth2User(savedUserId, "tester", "ROLE_USER")
        val auth = UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth
    }

    @Test
    @DisplayName("동화 생성 성공")
    fun t1() {
        val req = FairytaleCreateRequest(
            childName = "민지",
            childRole = "모험가",
            characters = "토끼, 거북이",
            place = "숲속 마을",
            lesson = "협동, 우정",
            mood = "따뜻한, 유쾌한"
        )

        // Gemini 스텁
        every { geminiClient.generateFairytale(match { it.contains("응답 형식: [제목:") }) } returns """
            [제목: 민지의 용감한 모험]
            민지는 숲속 마을에서 토끼와 거북이와 함께 협동의 가치를 배우는 모험을 떠났어요...
        """.trimIndent()
        every { geminiClient.generateFairytale(match { it.contains("영어로 번역") }) } returns
                "a cute child adventurer with a rabbit and a turtle, smiling, warm tone, 1:1 illustration"

        // HuggingFace/GCS 스텁
        every { huggingFaceClient.generateImage(any()) } returns "fake-image".toByteArray()
        every { googleCloudStorage.uploadImageBytesToCloud(any()) } returns
                "https://storage.googleapis.com/bucket/fairy.png"

        val mvcResult = mockMvc.perform(
            post("/fairytales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = mvcResult.response.contentAsString
        val res = objectMapper.readValue(body, FairytaleResponse::class.java)

        assertThat(res.id).isPositive()
        assertThat(res.title).isEqualTo("민지의 용감한 모험")
        assertThat(res.content).contains("민지", "숲속 마을", "협동")
        assertThat(res.imageUrl).isEqualTo("https://storage.googleapis.com/bucket/fairy.png")
        assertThat(res.childName).isEqualTo("민지")
        assertThat(res.childRole).isEqualTo("모험가")
        assertThat(res.characters).isEqualTo("토끼, 거북이")
        assertThat(res.place).isEqualTo("숲속 마을")
        assertThat(res.lesson).isEqualTo("협동, 우정")
        assertThat(res.mood).isEqualTo("따뜻한, 유쾌한")
        assertThat(res.userId).isEqualTo(savedUserId)
        assertThat(res.createdAt).isNotNull()
    }

    @Test
    @DisplayName("동화 생성 실패 - childName 공백(NotBlank 위반)")
    fun t2() {
        val req = FairytaleCreateRequest(
            childName = "   ",
            childRole = "모험가",
            characters = "토끼, 거북이",
            place = "숲속 마을",
            lesson = "협동, 우정",
            mood = "따뜻한, 유쾌한"
        )

        val mvcResult = mockMvc.perform(
            post("/fairytales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isBadRequest)
            .andReturn()

        // 외부 의존 호출이 전혀 없어야 함
        verify(exactly = 0) { geminiClient.generateFairytale(any()) }
        verify(exactly = 0) { huggingFaceClient.generateImage(any()) }
        verify(exactly = 0) { googleCloudStorage.uploadImageBytesToCloud(any()) }
        confirmVerified(geminiClient, huggingFaceClient, googleCloudStorage)
    }

    @Test
    @DisplayName("내 동화 전체 조회 성공")
    fun t3() {
        val user = userRepository.findById(savedUserId).get()

        // 동화 2개를 시간 차이를 두고 저장
        val f1 = fairytaleRepository.save(
            Fairytale(
                user = user,
                title = "첫 번째 동화",
                content = "내용1",
                imageUrl = null,
                isPublic = false
            )
        )
        Thread.sleep(10)
        val f2 = fairytaleRepository.save(
            Fairytale(
                user = user,
                title = "두 번째 동화",
                content = "내용2",
                imageUrl = null,
                isPublic = true
            )
        )

        val mvcResult = mockMvc.perform(
            get("/fairytales")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = mvcResult.response.contentAsString
        val res = objectMapper.readTree(body)

        assertThat(res.isArray).isTrue()
        assertThat(res.size()).isEqualTo(2)

        assertThat(res[0].get("title").asText()).isEqualTo("두 번째 동화")
        assertThat(res[1].get("title").asText()).isEqualTo("첫 번째 동화")
    }

    @Test
    @DisplayName("동화 상세 조회 성공")
    fun t4() {
        val user = userRepository.findById(savedUserId).get()

        // 동화 상세 조회는 키워드 필요
        val k1 = keywordRepository.save(Keyword.of("민지", KeywordType.CHILD_NAME))
        val k2 = keywordRepository.save(Keyword.of("모험가", KeywordType.CHILD_ROLE))
        val k3 = keywordRepository.save(Keyword.of("토끼", KeywordType.CHARACTERS))
        val k4 = keywordRepository.save(Keyword.of("거북이", KeywordType.CHARACTERS))
        val k5 = keywordRepository.save(Keyword.of("숲속 마을", KeywordType.PLACE))
        val k6 = keywordRepository.save(Keyword.of("협동, 우정", KeywordType.LESSON))
        val k7 = keywordRepository.save(Keyword.of("따뜻한, 유쾌한", KeywordType.MOOD))

        val fairy = Fairytale(
            user = user,
            title = "상세 테스트 동화",
            content = "내용 상세",
            imageUrl = "https://storage.googleapis.com/bucket/detail.png",
            isPublic = false
        )

        listOf(k1, k2, k3, k4, k5, k6, k7).forEach { fairy.addKeyword(it) }
        fairytaleRepository.save(fairy)

        val mvcResult = mockMvc.perform(
            get("/fairytales/{id}", fairy.id!!)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = mvcResult.response.contentAsString
        val res = objectMapper.readValue(body, FairytaleDetailResponse::class.java)

        assertThat(res.childName).isEqualTo("민지")
        assertThat(res.place).isEqualTo("숲속 마을")
        assertThat(res.id).isEqualTo(fairy.id)
        assertThat(res.title).isEqualTo("상세 테스트 동화")
        assertThat(res.content).isEqualTo("내용 상세")
        assertThat(res.imageUrl).isEqualTo("https://storage.googleapis.com/bucket/detail.png")
        assertThat(res.isPublic).isFalse()
        assertThat(res.createdAt).isNotNull()
    }

    @Test
    @DisplayName("동화 상세 조회 실패 - 존재하지 않는 ID")
    fun t5() {
        val notExistId = 999L

        val mvcResult = mockMvc.perform(
            get("/fairytales/{id}", notExistId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andReturn()

        val body = mvcResult.response.contentAsString
        assertThat(body).contains("동화를 찾을 수") // 서비스 메시지 일부만 느슨히 검증
    }

    @Test
    @DisplayName("동화 삭제 성공")
    fun t6() {
        val user = userRepository.findById(savedUserId).get()
        val imageUrl = "https://storage.googleapis.com/bucket/to-delete.png"
        val saved = fairytaleRepository.save(
            Fairytale(
                user = user,
                title = "삭제 대상 동화",
                content = "삭제 컨텐츠",
                imageUrl = imageUrl,
                isPublic = false
            )
        )

        mockMvc.perform(
            delete("/fairytales/{id}", saved.id!!)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNoContent)

        // DB도 검증
        val exists = fairytaleRepository.findById(saved.id!!).isPresent
        assertThat(exists).isFalse()

        // 이미지 삭제 호출되었는지
        verify(exactly = 1) { googleCloudStorage.deleteImages(match { it == listOf(imageUrl) }) }
    }

}
