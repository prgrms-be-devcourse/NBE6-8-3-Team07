package com.back.fairytale.domain.thoughts.controller

import com.back.fairytale.domain.fairytale.exception.FairytaleNotFoundException
import com.back.fairytale.domain.thoughts.dto.ThoughtsRequest
import com.back.fairytale.domain.thoughts.dto.ThoughtsResponse
import com.back.fairytale.domain.thoughts.dto.ThoughtsUpdateRequest
import com.back.fairytale.domain.thoughts.service.ThoughtsService
import com.back.fairytale.global.security.CustomOAuth2User
import com.back.fairytale.global.security.SecurityConfig
import com.back.fairytale.global.security.jwt.JwtAuthenticationFilter
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

// ThoughtsController에 대한 웹 계층 테스트 클래스
// Spring Security 설정을 제외하고 ThoughtsController만 로드하여 테스트
@WebMvcTest(
    controllers = [ThoughtsController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [SecurityConfig::class, JwtAuthenticationFilter::class]
        )
    ]
)
class ThoughtsControllerUnitTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var thoughtsService: ThoughtsService

    private val testUserId = 1L

    companion object {
        private const val testUserName = "testuser"
        private const val testUserRole = "USER"
    }

    private lateinit var mockCustomOAuth2User: CustomOAuth2User
    private lateinit var thoughtsRequest: ThoughtsRequest
    private lateinit var thoughtsResponse: ThoughtsResponse
    private lateinit var thoughtsUpdateRequest: ThoughtsUpdateRequest

    // 각 테스트 실행 전 초기 설정
    // 테스트에 필요한 Mock 객체 및 요청/응답 데이터 초기화
    @BeforeEach
    fun setUp() {
        // Mock CustomOAuth2User 객체 생성 (인증된 사용자 정보)
        mockCustomOAuth2User = CustomOAuth2User(
            id = testUserId,
            username = testUserName,
            role = testUserRole
        )

        // 아이생각 생성 요청 데이터
        thoughtsRequest = ThoughtsRequest(
            fairytaleId = 1L,
            name = "아이 이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )

        // 아이생각 응답 데이터
        thoughtsResponse = ThoughtsResponse(
            id = 1L,
            fairytaleId = 1L,
            userId = testUserId,
            userName = testUserName,
            name = "아이 이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용",
            createdAt = LocalDateTime.now()
        )

        // 아이생각 수정 요청 데이터
        thoughtsUpdateRequest = ThoughtsUpdateRequest(
            name = "수정된 이름",
            content = "수정된 내용",
            parentContent = "수정된 부모생각"
        )
    }

    @Nested
    @DisplayName("아이 생각 생성 API")
    inner class CreateThoughtsApi {
        // HTTP 201 Created 응답과 Location 헤더, 올바른 응답 본문을 확인
        @Test
        @DisplayName("POST /api/thoughts - 아이생각 작성 성공")
        fun createThoughtsSuccess() {
            // thoughtsService.createThoughts 호출 시 thoughtsResponse 반환하도록 Mock
            every { thoughtsService.createThoughts(thoughtsRequest, testUserId) } returns thoughtsResponse

            // POST 요청 수행 및 응답 검증
            mockMvc.perform(
                post("/api/thoughts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(thoughtsRequest))
                    .with(authentication(OAuth2AuthenticationToken(
                        mockCustomOAuth2User,
                        listOf(SimpleGrantedAuthority("ROLE_${testUserRole}")),
                        "oauth2"
                    ))) // 인증 정보 주입
                    .with(csrf()) // CSRF 토큰 주입 (POST, PUT, DELETE 요청에 필요)
            )
                .andExpect(status().isCreated) // HTTP 201 Created 확인
                .andExpect(header().string("Location", "/api/thoughts/1")) // Location 헤더 확인
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fairytaleId").value(1L))
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.userName").value(testUserName))
                .andExpect(jsonPath("$.name").value("아이 이름"))
                .andExpect(jsonPath("$.content").value("아이생각 내용"))
                .andExpect(jsonPath("$.parentContent").value("부모생각 내용"))

            // thoughtsService.createThoughts 메서드가 정확히 한 번 호출되었는지 검증
            verify { thoughtsService.createThoughts(thoughtsRequest, testUserId) }
        }

        // 서비스 계층에서 RuntimeException 발생 시 HTTP 500 Internal Server Error 응답을 확인
        @Test
        @DisplayName("POST /api/thoughts - 아이생각 작성 실패 (서비스 예외)")
        fun createThoughtsFailureServiceException() {
            // thoughtsService.createThoughts 호출 시 RuntimeException 발생하도록 Mock
            every { thoughtsService.createThoughts(any(), any()) } throws RuntimeException("Service error during creation")

            // POST 요청 수행 및 응답 검증
            mockMvc.perform(
                post("/api/thoughts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(thoughtsRequest))
                    .with(authentication(OAuth2AuthenticationToken(
                        mockCustomOAuth2User,
                        listOf(SimpleGrantedAuthority("ROLE_${testUserRole}")),
                        "oauth2"
                    ))) // 인증 정보 주입
                    .with(csrf()) // CSRF 토큰 주입
            )
                .andExpect(status().isInternalServerError) // HTTP 500 Internal Server Error 확인

            // thoughtsService.createThoughts 메서드가 정확히 한 번 호출되었는지 검증
            verify { thoughtsService.createThoughts(any(), any()) }
        }
    }

    @Nested
    @DisplayName("아이 생각 조회 API")
    inner class GetThoughtsApi {
        // HTTP 200 OK 응답과 올바른 응답 본문을 확인
        @Test
        @DisplayName("GET /api/thoughts/{id} - 아이생각 조회 성공")
        fun getThoughtsSuccess() {
            // thoughtsService.getThoughts 호출 시 thoughtsResponse 반환하도록 Mock
            every { thoughtsService.getThoughts(1L, testUserId) } returns thoughtsResponse

            // GET 요청 수행 및 응답 검증
            mockMvc.perform(
                get("/api/thoughts/1")
                    .with(authentication(OAuth2AuthenticationToken(
                        mockCustomOAuth2User,
                        listOf(SimpleGrantedAuthority("ROLE_${testUserRole}")),
                        "oauth2"
                    ))) // 인증 정보 주입
            )
                .andExpect(status().isOk) // HTTP 200 OK 확인
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fairytaleId").value(1L))
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.userName").value(testUserName))
                .andExpect(jsonPath("$.name").value("아이 이름"))
                .andExpect(jsonPath("$.content").value("아이생각 내용"))
                .andExpect(jsonPath("$.parentContent").value("부모생각 내용"))

            // thoughtsService.getThoughts 메서드가 정확히 한 번 호출되었는지 검증
            verify { thoughtsService.getThoughts(1L, testUserId) }
        }

         // thoughtsService에서 FairytaleNotFoundException 발생 시 HTTP 404 Not Found 응답을 확인
        @Test
        @DisplayName("GET /api/thoughts/{id} - 아이생각 조회 실패 (찾을 수 없음)")
        fun getThoughtsFailureNotFound() {
            // thoughtsService.getThoughts 호출 시 FairytaleNotFoundException 발생하도록 Mock
            every { thoughtsService.getThoughts(any(), any()) } throws FairytaleNotFoundException("Thought not found")

            // GET 요청 수행 및 응답 검증 (존재하지 않는 ID 사용)
            mockMvc.perform(
                get("/api/thoughts/999")
                    .with(authentication(OAuth2AuthenticationToken(
                        mockCustomOAuth2User,
                        listOf(SimpleGrantedAuthority("ROLE_${testUserRole}")),
                        "oauth2"
                    ))) // 인증 정보 주입
            )
                .andExpect(status().isNotFound) // HTTP 404 Not Found 확인

            // thoughtsService.getThoughts 메서드가 정확히 한 번 호출되었는지 검증
            verify { thoughtsService.getThoughts(any(), any()) }
        }
    }

    @Nested
    @DisplayName("아이 생각 수정 API")
    inner class UpdateThoughtsApi {
         // HTTP 200 OK 응답과 올바른 응답 본문을 확인
        @Test
        @DisplayName("PUT /api/thoughts/{id} - 아이생각 수정 성공")
        fun updateThoughtsSuccess() {
            val updatedResponse = thoughtsResponse.copy(
                name = "수정된 이름",
                content = "수정된 내용",
                parentContent = "수정된 부모생각"
            )
            // thoughtsService.updateThoughts 호출 시 수정된 응답 반환하도록 Mock
            every { thoughtsService.updateThoughts(1L, thoughtsUpdateRequest, testUserId) } returns updatedResponse

            // PUT 요청 수행 및 응답 검증
            mockMvc.perform(
                put("/api/thoughts/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(thoughtsUpdateRequest))
                    .with(authentication(OAuth2AuthenticationToken(
                        mockCustomOAuth2User,
                        listOf(SimpleGrantedAuthority("ROLE_${testUserRole}")),
                        "oauth2"
                    ))) // 인증 정보 주입
                    .with(csrf()) // CSRF 토큰 주입
            )
                .andExpect(status().isOk) // HTTP 200 OK 확인
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("수정된 이름"))
                .andExpect(jsonPath("$.content").value("수정된 내용"))
                .andExpect(jsonPath("$.parentContent").value("수정된 부모생각"))

            // thoughtsService.updateThoughts 메서드가 정확히 한 번 호출되었는지 검증
            verify { thoughtsService.updateThoughts(1L, thoughtsUpdateRequest, testUserId) }
        }

        // thoughtsService에서 FairytaleNotFoundException 발생 시 HTTP 404 Not Found 응답을 확인
        @Test
        @DisplayName("PUT /api/thoughts/{id} - 아이생각 수정 실패 (찾을 수 없음)")
        fun updateThoughtsFailureNotFound() {
            // thoughtsService.updateThoughts 호출 시 FairytaleNotFoundException 발생하도록 Mock
            every { thoughtsService.updateThoughts(any(), any(), any()) } throws FairytaleNotFoundException("Thought not found")

            // PUT 요청 수행 및 응답 검증 (존재하지 않는 ID 사용)
            mockMvc.perform(
                put("/api/thoughts/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(thoughtsUpdateRequest))
                    .with(authentication(OAuth2AuthenticationToken(
                        mockCustomOAuth2User,
                        listOf(SimpleGrantedAuthority("ROLE_${testUserRole}")),
                        "oauth2"
                    ))) // 인증 정보 주입
                    .with(csrf()) // CSRF 토큰 주입
            )
                .andExpect(status().isNotFound) // HTTP 404 Not Found 확인

            // thoughtsService.updateThoughts 메서드가 정확히 한 번 호출되었는지 검증
            verify { thoughtsService.updateThoughts(any(), any(), any()) }
        }
    }

    @Nested
    @DisplayName("아이 생각 삭제 API")
    inner class DeleteThoughtsApi {
        // HTTP 204 No Content 응답을 확인
        @Test
        @DisplayName("DELETE /api/thoughts/{id} - 아이생각 삭제 성공")
        fun deleteThoughtsSuccess() {
            // thoughtsService.deleteThoughts 호출 시 아무것도 반환하지 않도록 Mock
            every { thoughtsService.deleteThoughts(1L, testUserId) } returns Unit

            // DELETE 요청 수행 및 응답 검증
            mockMvc.perform(
                delete("/api/thoughts/1")
                    .with(authentication(OAuth2AuthenticationToken(
                        mockCustomOAuth2User,
                        listOf(SimpleGrantedAuthority("ROLE_${testUserRole}")),
                        "oauth2"
                    ))) // 인증 정보 주입
                    .with(csrf()) // CSRF 토큰 주입
            )
                .andExpect(status().isNoContent) // HTTP 204 No Content 확인

            // thoughtsService.deleteThoughts 메서드가 정확히 한 번 호출되었는지 검증
            verify { thoughtsService.deleteThoughts(1L, testUserId) }
        }

        // thoughtsService에서 FairytaleNotFoundException 발생 시 HTTP 404 Not Found 응답을 확인
        @Test
        @DisplayName("DELETE /api/thoughts/{id} - 아이생각 삭제 실패 (찾을 수 없음)")
        fun deleteThoughtsFailureNotFound() {
            // thoughtsService.deleteThoughts 호출 시 FairytaleNotFoundException 발생하도록 Mock
            every { thoughtsService.deleteThoughts(any(), any()) } throws FairytaleNotFoundException("Thought not found")

            // DELETE 요청 수행 및 응답 검증 (존재하지 않는 ID 사용)
            mockMvc.perform(
                delete("/api/thoughts/999")
                    .with(authentication(OAuth2AuthenticationToken(
                        mockCustomOAuth2User,
                        listOf(SimpleGrantedAuthority("ROLE_${testUserRole}")),
                        "oauth2"
                    ))) // 인증 정보 주입
                    .with(csrf()) // CSRF 토큰 주입
            )
                .andExpect(status().isNotFound) // HTTP 404 Not Found 확인

            // thoughtsService.deleteThoughts 메서드가 정확히 한 번 호출되었는지 검증
            verify { thoughtsService.deleteThoughts(any(), any()) }
        }
    }
}