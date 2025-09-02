package com.back.fairytale.domain.fairytale.controller

import com.back.fairytale.domain.fairytale.dto.FairytaleCreateRequest
import com.back.fairytale.domain.fairytale.dto.FairytaleResponse
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.security.CustomOAuth2User
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.storage.Storage
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class FairytaleControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @MockkBean lateinit var geminiClient: GeminiClient
    @MockkBean lateinit var huggingFaceClient: HuggingFaceClient
    @MockkBean(relaxed = true) lateinit var googleCloudStorage: GoogleCloudStorage
    @MockkBean lateinit var storage: Storage

    private var savedUserId: Long = -1L

    @BeforeEach
    fun setUpUser() {
        // 테스트용 유저 저장
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

        // when
        val mvcResult = mockMvc.perform(
            post("/fairytales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isOk)
            .andReturn()

        // then
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
}
