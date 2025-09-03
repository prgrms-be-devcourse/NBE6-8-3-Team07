package com.back.fairytale.domain.comments.controller

import com.back.BackendApplication
import com.back.fairytale.domain.comments.dto.CommentsRequest
import com.back.fairytale.domain.comments.dto.CommentsUpdateRequest
import com.back.fairytale.domain.comments.entity.Comments
import com.back.fairytale.domain.comments.repository.CommentsRepository
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.security.CustomOAuth2User
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.storage.Storage
import com.ninjasquad.springmockk.MockkBean
import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(
    classes = [BackendApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Transactional
class CommentsControllerIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var commentsRepository: CommentsRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var fairytaleRepository: FairytaleRepository

    // Spring Boot Test(Spring Context)에서 MockkBean을 사용하여 필요한 의존성 주입
    @MockkBean
    private lateinit var geminiClient: GeminiClient

    @MockkBean
    private lateinit var huggingFaceClient: HuggingFaceClient

    @MockkBean
    private lateinit var googleCloudStorage: GoogleCloudStorage

    @MockkBean
    private lateinit var storage: Storage

    private lateinit var mockMvc: MockMvc
    private lateinit var user: User
    private lateinit var otherUser: User
    private lateinit var fairytale: Fairytale
    private lateinit var mockCustomOAuth2User: CustomOAuth2User

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
            .build()

        commentsRepository.deleteAll()
        fairytaleRepository.deleteAll()
        userRepository.deleteAll()

        user = User(
            email = "test@naver.com",
            name = "홍길동",
            nickname = "길동",
            socialId = "1234"
        )
        userRepository.save(user)

        otherUser = User(
            email = "other@naver.com",
            name = "김철수",
            nickname = "철수",
            socialId = "5678"
        )
        userRepository.save(otherUser)

        fairytale = Fairytale(
            user = user,
            title = "테스트 동화",
            content = "옛날 옛적에...",
            imageUrl = "www.example.com/image.jpg"
        )
        fairytaleRepository.save(fairytale)

        mockCustomOAuth2User = CustomOAuth2User(
            id = user.id!!,
            username = user.name,
            role = "USER"
        )
    }

    @Test
    @DisplayName("POST /api/fairytales/{fairytaleId}/comments - 댓글 작성 성공")
    fun createComments_Success() {
        // Given
        val request = CommentsRequest(
            fairytaleId = fairytale.id!!,
            content = "재미있는 동화네요!",
            parentId = null
        )

        // When & Then
        mockMvc.perform(
            post("/api/fairytales/{fairytaleId}/comments", fairytale.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.fairytaleId").value(fairytale.id))
            .andExpect(jsonPath("$.nickname").value(user.nickname))
            .andExpect(jsonPath("$.content").value("재미있는 동화네요!"))
            .andExpect(jsonPath("$.parentId").isEmpty)
            .andExpect(jsonPath("$.depth").value(0))
            .andExpect(jsonPath("$.hasChildren").value(false))
            .andExpect(jsonPath("$.childrenCount").value(0))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    @DisplayName("POST /api/fairytales/{fairytaleId}/comments - 대댓글 작성 성공")
    fun createReply_Success() {
        // Given - 부모 댓글 먼저 생성
        val parentComment = Comments(
            fairytale = fairytale,
            user = user,
            content = "부모 댓글"
        )
        commentsRepository.save(parentComment)

        val otherUserOAuth2User = CustomOAuth2User(
            id = otherUser.id!!,
            username = otherUser.name,
            role = "USER"
        )

        val request = CommentsRequest(
            fairytaleId = fairytale.id!!,
            content = "대댓글입니다!",
            parentId = parentComment.id
        )

        // When & Then
        mockMvc.perform(
            post("/api/fairytales/{fairytaleId}/comments", fairytale.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(authentication(OAuth2AuthenticationToken(
                    otherUserOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.fairytaleId").value(fairytale.id))
            .andExpect(jsonPath("$.nickname").value(otherUser.nickname))
            .andExpect(jsonPath("$.content").value("대댓글입니다!"))
            .andExpect(jsonPath("$.parentId").value(parentComment.id))
            .andExpect(jsonPath("$.depth").value(1))
            .andExpect(jsonPath("$.hasChildren").value(false))
            .andExpect(jsonPath("$.childrenCount").value(0))
    }

    @Test
    @DisplayName("POST /api/fairytales/{fairytaleId}/comments - URL과 요청 본문 fairytaleId 불일치 실패")
    fun createComments_FairytaleIdMismatch() {
        // Given
        val request = CommentsRequest(
            fairytaleId = 999L, // URL과 다른 ID
            content = "댓글 내용",
            parentId = null
        )

        // When & Then
        mockMvc.perform(
            post("/api/fairytales/{fairytaleId}/comments", fairytale.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("POST /api/fairytales/{fairytaleId}/comments - 존재하지 않는 동화로 작성 실패")
    fun createComments_FairytaleNotFound() {
        // Given
        val request = CommentsRequest(
            fairytaleId = 999L,
            content = "댓글 내용",
            parentId = null
        )

        // When & Then
        mockMvc.perform(
            post("/api/fairytales/{fairytaleId}/comments", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("GET /api/fairytales/{fairytaleId}/comments - 댓글 조회 성공")
    fun getComments_Success() {
        // Given - 댓글들 생성
        val parentComment = Comments(
            fairytale = fairytale,
            user = user,
            content = "부모 댓글"
        )
        commentsRepository.save(parentComment)

        val replyComment = Comments(
            fairytale = fairytale,
            user = otherUser,
            content = "대댓글",
            parent = parentComment
        )
        commentsRepository.save(replyComment)

        // When & Then
        mockMvc.perform(
            get("/api/fairytales/{fairytaleId}/comments", fairytale.id)
                .param("page", "0")
                .param("size", "10")
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[?(@.content == '부모 댓글')]").exists())
            .andExpect(jsonPath("$.content[?(@.content == '대댓글')]").exists())
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    @DisplayName("PATCH /api/comments/{id} - 댓글 수정 성공")
    fun updateComments_Success() {
        // Given
        val comment = Comments(
            fairytale = fairytale,
            user = user,
            content = "원본 댓글"
        )
        commentsRepository.save(comment)

        val updateRequest = CommentsUpdateRequest(
            content = "수정된 댓글"
        )

        // When & Then
        mockMvc.perform(
            patch("/api/comments/{id}", comment.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(comment.id))
            .andExpect(jsonPath("$.content").value("수정된 댓글"))
            .andExpect(jsonPath("$.nickname").value(user.nickname))
    }

    @Test
    @DisplayName("PATCH /api/comments/{id} - 다른 사용자가 수정 실패")
    fun updateComments_Unauthorized() {
        // Given
        val comment = Comments(
            fairytale = fairytale,
            user = user,
            content = "원본 댓글"
        )
        commentsRepository.save(comment)

        val updateRequest = CommentsUpdateRequest(
            content = "수정된 댓글"
        )

        val otherUserOAuth2User = CustomOAuth2User(
            id = otherUser.id!!,
            username = otherUser.name,
            role = "USER"
        )

        // When & Then
        mockMvc.perform(
            patch("/api/comments/{id}", comment.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(authentication(OAuth2AuthenticationToken(
                    otherUserOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("DELETE /api/comments/{id} - 댓글 삭제 성공")
    fun deleteComments_Success() {
        // Given
        val comment = Comments(
            fairytale = fairytale,
            user = user,
            content = "삭제할 댓글"
        )
        commentsRepository.save(comment)

        // When & Then
        mockMvc.perform(
            delete("/api/comments/{id}", comment.id)
                .contentType(MediaType.APPLICATION_JSON)
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isNoContent)

        // 데이터베이스에서 삭제되었는지 확인
        assert(!commentsRepository.findById(comment.id!!).isPresent)
    }

    @Test
    @DisplayName("DELETE /api/comments/{id} - 다른 사용자가 삭제 실패")
    fun deleteComments_Unauthorized() {
        // Given
        val comment = Comments(
            fairytale = fairytale,
            user = user,
            content = "댓글"
        )
        commentsRepository.save(comment)

        val otherUserOAuth2User = CustomOAuth2User(
            id = otherUser.id!!,
            username = otherUser.name,
            role = "USER"
        )

        // When & Then
        mockMvc.perform(
            delete("/api/comments/{id}", comment.id)
                .contentType(MediaType.APPLICATION_JSON)
                .with(authentication(OAuth2AuthenticationToken(
                    otherUserOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isForbidden)

        // 데이터베이스에서 삭제되지 않았는지 확인
        assert(commentsRepository.findById(comment.id!!).isPresent)
    }

    @Test
    @DisplayName("PATCH /api/comments/{id} - 존재하지 않는 댓글 수정 실패")
    fun updateComments_NotFound() {
        // Given
        val updateRequest = CommentsUpdateRequest(
            content = "수정된 댓글"
        )

        // When & Then
        mockMvc.perform(
            patch("/api/comments/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("DELETE /api/comments/{id} - 존재하지 않는 댓글 삭제 실패")
    fun deleteComments_NotFound() {
        // When & Then
        mockMvc.perform(
            delete("/api/comments/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("POST /api/fairytales/{fairytaleId}/comments - 존재하지 않는 부모 댓글로 대댓글 작성 실패")
    fun createReply_ParentNotFound() {
        // Given
        val request = CommentsRequest(
            fairytaleId = fairytale.id!!,
            content = "대댓글 내용",
            parentId = 999L
        )

        // When & Then
        mockMvc.perform(
            post("/api/fairytales/{fairytaleId}/comments", fairytale.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isNotFound)
    }
}