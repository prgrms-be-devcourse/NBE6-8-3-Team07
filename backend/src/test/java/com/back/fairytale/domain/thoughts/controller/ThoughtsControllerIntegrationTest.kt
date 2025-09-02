package com.back.fairytale.domain.thoughts.controller

import com.back.BackendApplication
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.thoughts.dto.ThoughtsRequest
import com.back.fairytale.domain.thoughts.dto.ThoughtsUpdateRequest
import com.back.fairytale.domain.thoughts.entity.Thoughts
import com.back.fairytale.domain.thoughts.repository.ThoughtsRepository
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf

@SpringBootTest(
    classes = [BackendApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Transactional
class ThoughtsControllerIntegrationTest {


    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var thoughtsRepository: ThoughtsRepository

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

        thoughtsRepository.deleteAll()
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
    @DisplayName("POST /api/thoughts - 아이생각 작성 성공")
    fun createThoughts_Success() {
        // Given
        val request = ThoughtsRequest(
            fairytaleId = fairytale.id!!,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )

        // When & Then
        mockMvc.perform(
            post("/api/thoughts")
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
            .andExpect(jsonPath("$.userId").value(user.id))
            .andExpect(jsonPath("$.userName").value(user.name))
            .andExpect(jsonPath("$.name").value("아이이름"))
            .andExpect(jsonPath("$.content").value("아이생각 내용"))
            .andExpect(jsonPath("$.parentContent").value("부모생각 내용"))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    @DisplayName("POST /api/thoughts - 존재하지 않는 동화로 작성 실패")
    fun createThoughts_FairytaleNotFound() {
        // Given
        val request = ThoughtsRequest(
            fairytaleId = 999L,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )

        // When & Then
        mockMvc.perform(
            post("/api/thoughts")
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
    @DisplayName("GET /api/thoughts/{id} - 아이생각 조회 성공")
    fun getThoughts_Success() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        // When & Then
        mockMvc.perform(
            get("/api/thoughts/{id}", thoughts.id)
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(thoughts.id))
            .andExpect(jsonPath("$.fairytaleId").value(fairytale.id))
            .andExpect(jsonPath("$.userId").value(user.id))
            .andExpect(jsonPath("$.name").value("아이이름"))
            .andExpect(jsonPath("$.content").value("아이생각 내용"))
            .andExpect(jsonPath("$.parentContent").value("부모생각 내용"))
    }

    @Test
    @DisplayName("GET /api/thoughts/{id} - 다른 사용자의 아이생각 조회 실패")
    fun getThoughts_Unauthorized() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        val otherUserOAuth2User = CustomOAuth2User(
            id = otherUser.id!!,
            username = otherUser.name,
            role = "USER"
        )

        // When & Then
        mockMvc.perform(
            get("/api/thoughts/{id}", thoughts.id)
                .with(authentication(OAuth2AuthenticationToken(
                    otherUserOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("GET /api/thoughts/fairytale/{fairytaleId} - 동화별 아이생각 조회 성공")
    fun getThoughtsByFairytaleId_Success() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        // When & Then
        mockMvc.perform(
            get("/api/thoughts/fairytale/{fairytaleId}", fairytale.id)
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(thoughts.id))
            .andExpect(jsonPath("$.fairytaleId").value(fairytale.id))
            .andExpect(jsonPath("$.userId").value(user.id))
            .andExpect(jsonPath("$.name").value("아이이름"))
    }

    @Test
    @DisplayName("PUT /api/thoughts/{id} - 아이생각 수정 성공")
    fun updateThoughts_Success() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        val updateRequest = ThoughtsUpdateRequest(
            name = "수정된 아이이름",
            content = "수정된 아이생각 내용",
            parentContent = "수정된 부모생각 내용"
        )

        // When & Then
        mockMvc.perform(
            put("/api/thoughts/{id}", thoughts.id)
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
            .andExpect(jsonPath("$.id").value(thoughts.id))
            .andExpect(jsonPath("$.name").value("수정된 아이이름"))
            .andExpect(jsonPath("$.content").value("수정된 아이생각 내용"))
            .andExpect(jsonPath("$.parentContent").value("수정된 부모생각 내용"))
    }

    @Test
    @DisplayName("PUT /api/thoughts/{id} - 다른 사용자가 수정 실패")
    fun updateThoughts_Unauthorized() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        val updateRequest = ThoughtsUpdateRequest(
            name = "수정된 아이이름",
            content = "수정된 아이생각 내용",
            parentContent = "수정된 부모생각 내용"
        )

        val otherUserOAuth2User = CustomOAuth2User(
            id = otherUser.id!!,
            username = otherUser.name,
            role = "USER"
        )

        // When & Then
        mockMvc.perform(
            put("/api/thoughts/{id}", thoughts.id)
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
    @DisplayName("DELETE /api/thoughts/{id} - 아이생각 삭제 성공")
    fun deleteThoughts_Success() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        // When & Then
        mockMvc.perform(
            delete("/api/thoughts/{id}", thoughts.id)
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isNoContent)

        // 데이터베이스에서 삭제되었는지 확인
        assert(!thoughtsRepository.findById(thoughts.id!!).isPresent)
    }

    @Test
    @DisplayName("DELETE /api/thoughts/{id} - 다른 사용자가 삭제 실패")
    fun deleteThoughts_Unauthorized() {
        // Given
        val thoughts = Thoughts(
            fairytale = fairytale,
            user = user,
            name = "아이이름",
            content = "아이생각 내용",
            parentContent = "부모생각 내용"
        )
        thoughtsRepository.save(thoughts)

        val otherUserOAuth2User = CustomOAuth2User(
            id = otherUser.id!!,
            username = otherUser.name,
            role = "USER"
        )

        // When & Then
        mockMvc.perform(
            delete("/api/thoughts/{id}", thoughts.id)
                .with(authentication(OAuth2AuthenticationToken(
                    otherUserOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
                .with(csrf())
        )
            .andExpect(status().isForbidden)

        // 데이터베이스에서 삭제되지 않았는지 확인
        assert(thoughtsRepository.findById(thoughts.id!!).isPresent)
    }

    @Test
    @DisplayName("GET /api/thoughts/{id} - 존재하지 않는 아이생각 조회 실패")
    fun getThoughts_NotFound() {
        // When & Then
        mockMvc.perform(
            get("/api/thoughts/{id}", 999L)
                .with(authentication(OAuth2AuthenticationToken(
                    mockCustomOAuth2User,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                    "oauth2"
                )))
        )
            .andExpect(status().isNotFound)
    }
}