package com.back.fairytale.domain.fairytale.controller

import com.back.fairytale.domain.fairytale.dto.FairytaleCreateRequest
import com.back.fairytale.domain.fairytale.dto.FairytaleResponse
import com.back.fairytale.domain.fairytale.service.FairytaleService
import com.back.fairytale.global.security.CustomOAuth2User
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
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

    companion object {
        private const val USER_ID = 1L
        private const val USERNAME = "tester"
        private const val ROLE = "ROLE_USER"
        private val FIXED_TIME = LocalDateTime.of(2024, 1, 1, 10, 0)
    }

    @BeforeEach
    fun setUp() {
        val principal = CustomOAuth2User(id = USER_ID, username = USERNAME, role = ROLE)
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
            userId = USER_ID,
            createdAt = FIXED_TIME
        )
    }

    @Test
    @DisplayName("동화 생성 성공")
    fun t1() {
        val reqSlot = slot<FairytaleCreateRequest>()
        val userIdSlot = slot<Long>()

        every {
            fairytaleService.createFairytale(capture(reqSlot), capture(userIdSlot))
        } returns expectedResponse

        mockMvc.perform(
            post("/fairytales")
                .with(authentication(authentication))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(expectedResponse.id))
            .andExpect(jsonPath("$.title").value(expectedResponse.title))
            .andExpect(jsonPath("$.userId").value(USER_ID))

        verify(exactly = 1) { fairytaleService.createFairytale(any(), any()) }
        assertThat(reqSlot.captured.childName).isEqualTo(validRequest.childName)
        assertThat(userIdSlot.captured).isEqualTo(USER_ID)
    }
}