package com.back.fairytale.domain.fairytale.controller

import com.back.fairytale.domain.fairytale.dto.FairytaleCreateRequest
import com.back.fairytale.domain.fairytale.dto.FairytaleResponse
import com.back.fairytale.domain.fairytale.service.FairytaleService
import com.back.fairytale.global.security.CustomOAuth2User
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.MediaType
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(
    controllers = [FairytaleController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [
                com.back.fairytale.global.security.SecurityConfig::class,
                com.back.fairytale.global.security.jwt.JwtAuthenticationFilter::class
            ]
        )
    ]
)
@ActiveProfiles("test")
class FairytaleControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    // MockK 기반의 MockkBean 사용
    @MockkBean
    private lateinit var fairytaleService: FairytaleService

    private lateinit var authentication: Authentication
    private lateinit var validRequest: FairytaleCreateRequest
    private lateinit var expectedResponse: FairytaleResponse

    @BeforeEach
    fun setUp() {
        val principal = CustomOAuth2User(
            id = 1L,
            username = "tester",
            role = "ROLE_USER"
        )
        authentication = TestingAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        ).apply { isAuthenticated = true }

        // 유효한 요청 데이터 설정
        validRequest = FairytaleCreateRequest(
            childName = "철수",
            childRole = "용감한 기사",
            characters = "마법사, 공주, 드래곤",
            place = "신비한 숲, 마법의 성",
            lesson = "용기, 우정",
            mood = "모험적인, 따뜻한"
        )

        // 예상 응답 데이터 설정
        expectedResponse = FairytaleResponse(
            id = 1L,
            title = "용감한 기사 철수의 모험",
            content = "옛날 옛적에 용감한 기사 철수가 살았습니다...",
            imageUrl = "https://example.com/image.jpg",
            childName = "철수",
            childRole = "용감한 기사",
            characters = "마법사, 공주, 드래곤",
            place = "신비한 숲, 마법의 성",
            lesson = "용기, 우정",
            mood = "모험적인, 따뜻한",
            userId = 1L,
            createdAt = LocalDateTime.of(2024, 1, 1, 10, 0)
        )
    }

    @Test
    @DisplayName("동화 생성 성공")
    @WithMockUser
    fun t1() {
        // given - MockK의 자연스러운 DSL 사용
        every {
            fairytaleService.createFairytale(any<FairytaleCreateRequest>(), eq(1L))
        } returns expectedResponse

        // when & then
        val result = mockMvc.perform(
            post("/fairytales")
                .with(authentication(authentication))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()

        // 응답 검증
        val responseBody = result.response.contentAsString
        val actualResponse = objectMapper.readValue(responseBody, FairytaleResponse::class.java)

        assertThat(actualResponse).isNotNull
        assertThat(actualResponse.id).isEqualTo(expectedResponse.id)
        assertThat(actualResponse.title).isEqualTo(expectedResponse.title)
        assertThat(actualResponse.content).isEqualTo(expectedResponse.content)
        assertThat(actualResponse.imageUrl).isEqualTo(expectedResponse.imageUrl)
        assertThat(actualResponse.childName).isEqualTo(expectedResponse.childName)
        assertThat(actualResponse.childRole).isEqualTo(expectedResponse.childRole)
        assertThat(actualResponse.characters).isEqualTo(expectedResponse.characters)
        assertThat(actualResponse.place).isEqualTo(expectedResponse.place)
        assertThat(actualResponse.lesson).isEqualTo(expectedResponse.lesson)
        assertThat(actualResponse.mood).isEqualTo(expectedResponse.mood)
        assertThat(actualResponse.userId).isEqualTo(expectedResponse.userId)
        assertThat(actualResponse.createdAt).isEqualTo(expectedResponse.createdAt)

        // MockK를 사용한 호출 검증 - 더 읽기 쉬운 문법
        verify(exactly = 1) {
            fairytaleService.createFairytale(any<FairytaleCreateRequest>(), eq(1L))
        }
    }
}