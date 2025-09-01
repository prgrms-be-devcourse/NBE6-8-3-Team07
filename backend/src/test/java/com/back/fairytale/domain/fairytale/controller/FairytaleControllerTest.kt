package com.back.fairytale.domain.fairytale.controller

import com.back.fairytale.domain.fairytale.dto.FairytaleCreateRequest
import com.back.fairytale.domain.fairytale.dto.FairytaleDetailResponse
import com.back.fairytale.domain.fairytale.dto.FairytaleListResponse
import com.back.fairytale.domain.fairytale.dto.FairytaleResponse
import com.back.fairytale.domain.fairytale.exception.FairytaleNotFoundException
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
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
    }

    @Test
    @DisplayName("동화 생성 성공")
    fun t1() {
        val reqSlot = slot<FairytaleCreateRequest>()
        val userIdSlot = slot<Long>()

        val validRequest = FairytaleCreateRequest(
            childName = "철수",
            childRole = "용감한 기사",
            characters = "마법사, 공주, 드래곤",
            place = "신비한 숲, 마법의 성",
            lesson = "용기, 우정",
            mood = "모험적인, 따뜻한"
        )

        val expectedResponse = FairytaleResponse(
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

    @Test
    @DisplayName("동화 생성 실패: 검증 에러")
    fun t2() {
        // 유효하지 않은 요청(childName 빈 문자열)
        val invalidRequest = FairytaleCreateRequest(
            childName = "",
            childRole = "용감한 기사",
            characters = "마법사, 공주, 드래곤",
            place = "신비한 숲, 마법의 성",
            lesson = "용기, 우정",
            mood = "모험적인, 따뜻한"
        )

        mockMvc.perform(
            post("/fairytales")
                .with(authentication(authentication))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)

        // 서비스는 호출되면 안 됨 (유효성 검증 실패했으니까)
        verify(exactly = 0) { fairytaleService.createFairytale(any(), any()) }
    }

    @Test
    @DisplayName("동화 전체 조회 성공")
    fun t3() {
        val listResponse = listOf(
            FairytaleListResponse(
                id = 1L,
                title = "용감한 기사 철수의 모험",
                imageUrl = "https://example.com/image.jpg",
                isPublic = true,
                createdAt = FIXED_TIME.toLocalDate()
            ),
            FairytaleListResponse(
                id = 2L,
                title = "두번째 동화",
                imageUrl = null,
                isPublic = false,
                createdAt = FIXED_TIME.toLocalDate()
            )
        )

        val userIdSlot = slot<Long>()
        every {
            fairytaleService.getAllFairytalesByUserId(capture(userIdSlot))
        } returns listResponse

        mockMvc.perform(get("/fairytales").with(authentication(authentication)))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].title").value("용감한 기사 철수의 모험"))
            .andExpect(jsonPath("$[0].isPublic").value(true))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].title").value("두번째 동화"))
            .andExpect(jsonPath("$[1].isPublic").value(false))

        verify(exactly = 1) { fairytaleService.getAllFairytalesByUserId(any()) }
        assertThat(userIdSlot.captured).isEqualTo(USER_ID)
    }

    @Test
    @DisplayName("동화 전체 조회: 동화 없음")
    fun t4() {
        every {
            fairytaleService.getAllFairytalesByUserId(any())
        } throws FairytaleNotFoundException("등록된 동화가 없습니다.")

        mockMvc.perform(get("/fairytales").with(authentication(authentication)))
            .andExpect(status().isNotFound)
            .andExpect(content().string("등록된 동화가 없습니다."))

        verify(exactly = 1) { fairytaleService.getAllFairytalesByUserId(any()) }
    }

    @Test
    @DisplayName("동화 상세 조회 성공")
    fun t5() {
        val id = 1L
        val idSlot = slot<Long>()
        val userIdSlot = slot<Long>()

        val detailResponse = FairytaleDetailResponse(
            id = id,
            title = "용감한 기사 철수의 모험",
            content = "옛날 옛적에...",
            imageUrl = "https://example.com/image.jpg",
            isPublic = true,
            childName = "철수",
            childRole = "용감한 기사",
            characters = "마법사, 공주, 드래곤",
            place = "신비한 숲, 마법의 성",
            lesson = "용기, 우정",
            mood = "모험적인, 따뜻한",
            createdAt = FIXED_TIME
        )

        every {
            fairytaleService.getFairytaleByIdAndUserId(capture(idSlot), capture(userIdSlot))
        } returns detailResponse

        // when & then
        mockMvc.perform(
            get("/fairytales/{id}", id).with(authentication(authentication))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.title").value("용감한 기사 철수의 모험"))
            .andExpect(jsonPath("$.childName").value("철수"))
            .andExpect(jsonPath("$.childRole").value("용감한 기사"))
            .andExpect(jsonPath("$.isPublic").value(true))

        // verify + slot 검증
        verify(exactly = 1) { fairytaleService.getFairytaleByIdAndUserId(any(), any()) }
        assertThat(idSlot.captured).isEqualTo(id)
        assertThat(userIdSlot.captured).isEqualTo(USER_ID)
    }

    @Test
    @DisplayName("동화 상세 조회 실패: 동화 없음")
    fun t6() {
        every {
            fairytaleService.getFairytaleByIdAndUserId(any(), any())
        } throws FairytaleNotFoundException("동화를 찾을 수 없습니다.")

        mockMvc.perform(
            get("/fairytales/{id}", 999L).with(authentication(authentication))
        )
            .andExpect(status().isNotFound)
            .andExpect(content().string("동화를 찾을 수 없습니다."))

        verify(exactly = 1) { fairytaleService.getFairytaleByIdAndUserId(any(), any()) }
    }

    @Test
    @DisplayName("동화 삭제 성공")
    fun t7() {
        val idSlot = slot<Long>()
        val userIdSlot = slot<Long>()

        every {
            fairytaleService.deleteFairytaleByIdAndUserId(capture(idSlot), capture(userIdSlot))
        } returns Unit

        mockMvc.perform(
            delete("/fairytales/{id}", 1L)
                .with(authentication(authentication))
                .with(csrf())
        )
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))

        verify(exactly = 1) { fairytaleService.deleteFairytaleByIdAndUserId(any(), any()) }
        assertThat(idSlot.captured).isEqualTo(1L)
        assertThat(userIdSlot.captured).isEqualTo(USER_ID)
    }

    @Test
    @DisplayName("동화 삭제 실패: 동화 없음")
    fun t8() {
        every {
            fairytaleService.deleteFairytaleByIdAndUserId(any(), any())
        } throws FairytaleNotFoundException("삭제할 동화를 찾을 수 없습니다.")

        mockMvc.perform(
            delete("/fairytales/{id}", 999L)
                .with(authentication(authentication))
                .with(csrf())
        )
            .andExpect(status().isNotFound)
            .andExpect(content().string("삭제할 동화를 찾을 수 없습니다."))

        verify(exactly = 1) { fairytaleService.deleteFairytaleByIdAndUserId(any(), any()) }
    }

}