package com.back.fairytale.domain.comments.controller

import com.back.fairytale.domain.comments.dto.CommentsRequest
import com.back.fairytale.domain.comments.dto.CommentsResponse
import com.back.fairytale.domain.comments.dto.CommentsUpdateRequest
import com.back.fairytale.domain.comments.service.CommentsService
import com.back.fairytale.global.security.CustomOAuth2User
import com.back.fairytale.global.security.SecurityConfig
import com.back.fairytale.global.security.jwt.JwtAuthenticationFilter
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

// CommentsController에 대한 웹 계층 테스트 클래스
// Spring Security 설정을 제외하고 CommentsController만 로드하여 테스트
@WebMvcTest(
    controllers = [CommentsController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [SecurityConfig::class, JwtAuthenticationFilter::class]
        )
    ]
)
@AutoConfigureMockMvc(addFilters = false)
class CommentsControllerUnitTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var commentsService: CommentsService

    private val testUserId = 1L
    private val testFairytaleId = 1L

    companion object {
        private const val testUserName = "testuser"
        private const val testUserRole = "USER"
        private const val testNickname = "testnick"
    }

    private lateinit var mockCustomOAuth2User: CustomOAuth2User
    private lateinit var commentsRequest: CommentsRequest
    private lateinit var commentsResponse: CommentsResponse
    private lateinit var commentsUpdateRequest: CommentsUpdateRequest

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
        
        // SecurityContext에 Authentication 설정 (@AuthenticationPrincipal 파라미터 주입을 위해 필요)
        val authentication = OAuth2AuthenticationToken(
            mockCustomOAuth2User,
            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
            "oauth2"
        )
        SecurityContextHolder.getContext().authentication = authentication

        // 댓글 생성 요청 데이터
        commentsRequest = CommentsRequest(
            fairytaleId = testFairytaleId,
            content = "테스트 댓글 내용"
        )

        // 댓글 응답 데이터
        commentsResponse = CommentsResponse(
            id = 1L,
            fairytaleId = testFairytaleId,
            nickname = testNickname,
            content = "테스트 댓글 내용",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            parentId = null,
            depth = 0,
            hasChildren = false,
            childrenCount = 0
        )

        // 댓글 수정 요청 데이터
        commentsUpdateRequest = CommentsUpdateRequest(
            content = "수정된 댓글 내용"
        )
    }

    @Nested
    @DisplayName("댓글 작성 API")
    inner class CreateCommentsApi {
        
        // HTTP 201 Created 응답과 Location 헤더, 올바른 응답 본문을 확인
        @Test
        @DisplayName("POST /api/fairytales/{fairytaleId}/comments - 댓글 작성 성공")
        fun createCommentsSuccess() {
            // commentsService.createComments 호출 시 commentsResponse 반환하도록 Mock
            every { commentsService.createComments(commentsRequest, testUserId) } returns commentsResponse

            // POST 요청 수행 및 응답 검증
            mockMvc.perform(
                post("/api/fairytales/$testFairytaleId/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentsRequest))
                    .with { request -> // 요청에 인증 정보 주입
                        val auth = OAuth2AuthenticationToken(
                            mockCustomOAuth2User,
                            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
                            "oauth2"
                        )
                        request.userPrincipal = auth
                        SecurityContextHolder.getContext().authentication = auth
                        request
                    }
                    .with(csrf()) // CSRF 토큰 주입 (POST, PUT, DELETE 요청에 필요)
            )
                .andExpect(status().isCreated) // HTTP 201 Created 확인
                .andExpect(header().string("Location", "/api/fairytales/$testFairytaleId/comments/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fairytaleId").value(testFairytaleId))
                .andExpect(jsonPath("$.nickname").value(testNickname))
                .andExpect(jsonPath("$.content").value("테스트 댓글 내용"))
                .andExpect(jsonPath("$.parentId").isEmpty)
                .andExpect(jsonPath("$.depth").value(0))
                .andExpect(jsonPath("$.hasChildren").value(false))
                .andExpect(jsonPath("$.childrenCount").value(0))

            // commentsService.createComments 메서드가 정확히 한 번 호출되었는지 검증
            verify { commentsService.createComments(commentsRequest, testUserId) }
        }

        // 서비스 계층에서 RuntimeException 발생 시 HTTP 500 Internal Server Error 응답을 확인
        @Test
        @DisplayName("POST /api/fairytales/{fairytaleId}/comments - 댓글 작성 실패 (서비스 예외)")
        fun createCommentsFailureServiceException() {
            // commentsService.createComments 호출 시 RuntimeException 발생하도록 Mock
            every { commentsService.createComments(any(), any()) } throws RuntimeException("Service error")

            // POST 요청 수행 및 응답 검증
            mockMvc.perform(
                post("/api/fairytales/$testFairytaleId/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentsRequest))
                    .with { request -> // 요청에 인증 정보 주입
                        val auth = OAuth2AuthenticationToken(
                            mockCustomOAuth2User,
                            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
                            "oauth2"
                        )
                        request.userPrincipal = auth
                        SecurityContextHolder.getContext().authentication = auth
                        request
                    }
                    .with(csrf())
            )
                .andExpect(status().isInternalServerError) // HTTP 500 Internal Server Error 확인

            // commentsService.createComments 메서드가 정확히 한 번 호출되었는지 검증
            verify { commentsService.createComments(any(), any()) }
        }

        @Test
        @DisplayName("POST /api/fairytales/{fairytaleId}/comments - 대댓글 작성 성공")
        fun createReplySuccess() {
            // 대댓글 요청 데이터 (parentId 포함)
            val replyRequest = CommentsRequest(
                fairytaleId = testFairytaleId,
                content = "대댓글 내용입니다",
                parentId = 1L
            )

            // 대댓글 응답 데이터
            val replyResponse = CommentsResponse(
                id = 2L,
                fairytaleId = testFairytaleId,
                nickname = testNickname,
                content = "대댓글 내용입니다",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                parentId = 1L,
                depth = 1,
                hasChildren = false,
                childrenCount = 0
            )

            every { commentsService.createComments(replyRequest, testUserId) } returns replyResponse

            mockMvc.perform(
                post("/api/fairytales/$testFairytaleId/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(replyRequest))
                    .with { request ->
                        val auth = OAuth2AuthenticationToken(
                            mockCustomOAuth2User,
                            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
                            "oauth2"
                        )
                        request.userPrincipal = auth
                        SecurityContextHolder.getContext().authentication = auth
                        request
                    }
                    .with(csrf())
            )
                .andExpect(status().isCreated)
                .andExpect(header().string("Location", "/api/fairytales/$testFairytaleId/comments/2"))
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.parentId").value(1L))
                .andExpect(jsonPath("$.depth").value(1))
                .andExpect(jsonPath("$.hasChildren").value(false))
                .andExpect(jsonPath("$.content").value("대댓글 내용입니다"))

            verify { commentsService.createComments(replyRequest, testUserId) }
        }

        @Test
        @DisplayName("POST /api/fairytales/{fairytaleId}/comments - URL과 요청 본문의 동화Id 일치확인 실패")
        fun createCommentsFailureFairytaleIdMismatch() {
            // URL의 fairytaleId와 요청 body의 fairytaleId가 다른 경우
            val mismatchRequest = commentsRequest.copy(fairytaleId = 999L)

            mockMvc.perform(
                post("/api/fairytales/$testFairytaleId/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mismatchRequest))
                    .with { request ->
                        val auth = OAuth2AuthenticationToken(
                            mockCustomOAuth2User,
                            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
                            "oauth2"
                        )
                        request.userPrincipal = auth
                        SecurityContextHolder.getContext().authentication = auth
                        request
                    }
                    .with(csrf())
            )
                .andExpect(status().isBadRequest)

            // Service 메서드는 호출되지 않아야 함 (Controller에서 검증 실패)
            verify(exactly = 0) { commentsService.createComments(any(), any()) }
        }
    }

    @Nested
    @DisplayName("댓글 조회 API")
    inner class GetCommentsApi {
        
        // HTTP 200 OK 응답과 올바른 응답 본문을 확인
        @Test
        @DisplayName("GET /api/fairytales/{fairytaleId}/comments - 댓글 조회 성공")
        fun getCommentsSuccess() {
            val pageable = PageRequest.of(0, 10)
            val commentsPage = PageImpl(listOf(commentsResponse), pageable, 1)
            // commentsService.getCommentsByFairytale 호출 시 commentsPage 반환하도록 Mock
            every { commentsService.getCommentsByFairytale(testFairytaleId, any()) } returns commentsPage

            // GET 요청 수행 및 응답 검증
            mockMvc.perform(
                get("/api/fairytales/$testFairytaleId/comments")
                    .param("page", "0")
                    .param("size", "10")
                    .with { request -> // 요청에 인증 정보 주입
                        val auth = OAuth2AuthenticationToken(
                            mockCustomOAuth2User,
                            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
                            "oauth2"
                        )
                        request.userPrincipal = auth
                        SecurityContextHolder.getContext().authentication = auth
                        request
                    }
            )
                .andExpect(status().isOk) // HTTP 200 OK 확인
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].fairytaleId").value(testFairytaleId))
                .andExpect(jsonPath("$.content[0].nickname").value(testNickname))
                .andExpect(jsonPath("$.content[0].content").value("테스트 댓글 내용"))

            // commentsService.getCommentsByFairytale 메서드가 정확히 한 번 호출되었는지 검증
            verify { commentsService.getCommentsByFairytale(testFairytaleId, any()) }
        }

        // commentsService에서 RuntimeException 발생 시 HTTP 500 Internal Server Error 응답을 확인
        @Test
        @DisplayName("GET /api/fairytales/{fairytaleId}/comments - 댓글 조회 실패 (서비스 예외)")
        fun getCommentsFailureServiceException() {
            // commentsService.getCommentsByFairytale 호출 시 RuntimeException 발생하도록 Mock
            every { commentsService.getCommentsByFairytale(any(), any()) } throws RuntimeException("Service error")

            // GET 요청 수행 및 응답 검증
            mockMvc.perform(
                get("/api/fairytales/$testFairytaleId/comments")
                    .param("page", "0")
                    .param("size", "10")
                    .with { request -> // 요청에 인증 정보 주입
                        val auth = OAuth2AuthenticationToken(
                            mockCustomOAuth2User,
                            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
                            "oauth2"
                        )
                        request.userPrincipal = auth
                        SecurityContextHolder.getContext().authentication = auth
                        request
                    }
            )
                // then: HTTP 500 응답 검증
                .andExpect(status().isInternalServerError)

            // 메서드 호출 검증
            verify { commentsService.getCommentsByFairytale(any(), any()) }
        }

        @Test
        @DisplayName("GET /api/fairytales/{fairytaleId}/comments - 계층 구조 댓글 조회 성공")
        fun getHierarchicalCommentsSuccess() {
            val pageable = PageRequest.of(0, 10)
            
            // 부모 댓글과 대댓글이 포함된 응답 데이터
            val parentComment = CommentsResponse(
                id = 1L,
                fairytaleId = testFairytaleId,
                nickname = testNickname,
                content = "부모 댓글",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                parentId = null,
                depth = 0,
                hasChildren = true,
                childrenCount = 2
            )
            
            val replyComment1 = CommentsResponse(
                id = 2L,
                fairytaleId = testFairytaleId,
                nickname = "reply_user",
                content = "첫번째 대댓글",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                parentId = 1L,
                depth = 1,
                hasChildren = false,
                childrenCount = 0
            )
            
            val replyComment2 = CommentsResponse(
                id = 3L,
                fairytaleId = testFairytaleId,
                nickname = "reply_user2",
                content = "두번째 대댓글",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                parentId = 1L,
                depth = 1,
                hasChildren = false,
                childrenCount = 0
            )
            
            val commentsPage = PageImpl(listOf(parentComment, replyComment1, replyComment2), pageable, 3)
            every { commentsService.getCommentsByFairytale(testFairytaleId, any()) } returns commentsPage

            mockMvc.perform(
                get("/api/fairytales/$testFairytaleId/comments")
                    .param("page", "0")
                    .param("size", "10")
                    .with { request ->
                        val auth = OAuth2AuthenticationToken(
                            mockCustomOAuth2User,
                            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
                            "oauth2"
                        )
                        request.userPrincipal = auth
                        SecurityContextHolder.getContext().authentication = auth
                        request
                    }
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content.length()").value(3))
                // 부모 댓글 검증
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].parentId").isEmpty)
                .andExpect(jsonPath("$.content[0].depth").value(0))
                .andExpect(jsonPath("$.content[0].hasChildren").value(true))
                .andExpect(jsonPath("$.content[0].childrenCount").value(2))
                // 첫 번째 대댓글 검증
                .andExpect(jsonPath("$.content[1].id").value(2L))
                .andExpect(jsonPath("$.content[1].parentId").value(1L))
                .andExpect(jsonPath("$.content[1].depth").value(1))
                .andExpect(jsonPath("$.content[1].hasChildren").value(false))
                .andExpect(jsonPath("$.content[1].childrenCount").value(0))
                // 두 번째 대댓글 검증
                .andExpect(jsonPath("$.content[2].id").value(3L))
                .andExpect(jsonPath("$.content[2].parentId").value(1L))
                .andExpect(jsonPath("$.content[2].depth").value(1))

            verify { commentsService.getCommentsByFairytale(testFairytaleId, any()) }
        }
    }

    @Nested
    @DisplayName("댓글 수정 API")
    inner class UpdateCommentsApi {
        
        // HTTP 200 OK 응답과 올바른 응답 본문을 확인
        @Test
        @DisplayName("PATCH /api/comments/{id} - 댓글 수정 성공")
        fun updateCommentsSuccess() {
            val updatedResponse = commentsResponse.copy(content = "수정된 댓글 내용")
            // commentsService.updateComments 호출 시 수정된 응답 반환하도록 Mock
            every { commentsService.updateComments(1L, commentsUpdateRequest, testUserId) } returns updatedResponse

            // PATCH 요청 수행 및 응답 검증
            mockMvc.perform(
                patch("/api/comments/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentsUpdateRequest))
                    .with { request -> // 요청에 인증 정보 주입
                        val auth = OAuth2AuthenticationToken(
                            mockCustomOAuth2User,
                            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
                            "oauth2"
                        )
                        request.userPrincipal = auth
                        SecurityContextHolder.getContext().authentication = auth
                        request
                    }
                    .with(csrf()) // CSRF 토큰 주입
            )
                .andExpect(status().isOk) // HTTP 200 OK 확인
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value("수정된 댓글 내용"))

            // commentsService.updateComments 메서드가 정확히 한 번 호출되었는지 검증
            verify { commentsService.updateComments(1L, commentsUpdateRequest, testUserId) }
        }

        // commentsService에서 RuntimeException 발생 시 HTTP 500 Internal Server Error 응답을 확인
        @Test
        @DisplayName("PATCH /api/comments/{id} - 댓글 수정 실패 (찾을 수 없음)")
        fun updateCommentsFailureNotFound() {
            // commentsService.updateComments 호출 시 RuntimeException 발생하도록 Mock
            every { commentsService.updateComments(any(), any(), any()) } throws RuntimeException("Comment not found")

            // PATCH 요청 수행 및 응답 검증 (존재하지 않는 ID 사용)
            mockMvc.perform(
                patch("/api/comments/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentsUpdateRequest))
                    .with { request -> // 요청에 인증 정보 주입
                        val auth = OAuth2AuthenticationToken(
                            mockCustomOAuth2User,
                            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
                            "oauth2"
                        )
                        request.userPrincipal = auth
                        SecurityContextHolder.getContext().authentication = auth
                        request
                    }
                    .with(csrf())
            )
                // then: HTTP 500 응답 검증
                .andExpect(status().isInternalServerError)

            // 메서드 호출 검증
            verify { commentsService.updateComments(any(), any(), any()) }
        }
    }

    @Nested
    @DisplayName("댓글 삭제 API")
    inner class DeleteCommentsApi {
        
        // HTTP 204 No Content 응답을 확인
        @Test
        @DisplayName("DELETE /api/comments/{id} - 댓글 삭제 성공")
        fun deleteCommentsSuccess() {
            // commentsService.deleteComments 호출 시 아무것도 반환하지 않도록 Mock
            every { commentsService.deleteComments(1L, testUserId) } returns Unit

            // DELETE 요청 수행 및 응답 검증
            mockMvc.perform(
                delete("/api/comments/1")
                    .with { request -> // 요청에 인증 정보 주입
                        val auth = OAuth2AuthenticationToken(
                            mockCustomOAuth2User,
                            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
                            "oauth2"
                        )
                        request.userPrincipal = auth
                        SecurityContextHolder.getContext().authentication = auth
                        request
                    }
                    .with(csrf()) // CSRF 토큰 주입
            )
                // then: HTTP 204 No Content 응답 검증
                .andExpect(status().isNoContent)

            // 메서드 호출 검증
            verify { commentsService.deleteComments(1L, testUserId) }
        }

        // commentsService에서 RuntimeException 발생 시 HTTP 500 Internal Server Error 응답을 확인
        @Test
        @DisplayName("DELETE /api/comments/{id} - 댓글 삭제 실패 (찾을 수 없음)")
        fun deleteCommentsFailureNotFound() {
            // commentsService.deleteComments 호출 시 RuntimeException 발생하도록 Mock
            every { commentsService.deleteComments(any(), any()) } throws RuntimeException("Comment not found")

            // DELETE 요청 수행 및 응답 검증 (존재하지 않는 ID 사용)
            mockMvc.perform(
                delete("/api/comments/999")
                    .with { request -> // 요청에 인증 정보 주입
                        val auth = OAuth2AuthenticationToken(
                            mockCustomOAuth2User,
                            listOf(SimpleGrantedAuthority("ROLE_$testUserRole")),
                            "oauth2"
                        )
                        request.userPrincipal = auth
                        SecurityContextHolder.getContext().authentication = auth
                        request
                    }
                    .with(csrf())
            )
                // then: HTTP 500 응답 검증
                .andExpect(status().isInternalServerError)

            // 메서드 호출 검증
            verify { commentsService.deleteComments(any(), any()) }
        }
    }
}