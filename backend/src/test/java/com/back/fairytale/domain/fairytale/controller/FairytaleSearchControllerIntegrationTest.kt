package com.back.fairytale.domain.fairytale.controller

import com.back.BackendApplication
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(
    classes = [BackendApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Transactional
class FairytaleSearchControllerIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var fairytaleRepository: FairytaleRepository

    @Autowired
    private lateinit var userRepository: UserRepository

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
    private lateinit var fairytale1: Fairytale
    private lateinit var fairytale2: Fairytale
    private lateinit var fairytale3: Fairytale

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build()

        fairytaleRepository.deleteAll()
        userRepository.deleteAll()

        user = User(
            email = "test@naver.com",
            name = "홍길동",
            nickname = "길동",
            socialId = "1234"
        )
        userRepository.save(user)

        // 테스트용 동화 데이터 생성
        fairytale1 = Fairytale(
            user = user,
            title = "토끼와 거북이",
            content = "옛날 옛적에 토끼와 거북이가 살았습니다. 토끼는 빨랐지만 게을렀고, 거북이는 느렸지만 꾸준했습니다.",
            imageUrl = "https://example.com/rabbit-turtle.jpg"
        )
        fairytaleRepository.save(fairytale1)

        fairytale2 = Fairytale(
            user = user,
            title = "백설공주",
            content = "아름다운 백설공주가 일곱 난쟁이와 함께 살았습니다.",
            imageUrl = "https://example.com/snow-white.jpg"
        )
        fairytaleRepository.save(fairytale2)

        fairytale3 = Fairytale(
            user = user,
            title = "신데렐라",
            content = "계모와 언니들이 괴롭혔지만 착한 신데렐라는 왕자님을 만나 공주가 되었습니다.",
            imageUrl = "https://example.com/cinderella.jpg"
        )
        fairytaleRepository.save(fairytale3)
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 제목으로 검색 성공")
    fun searchFairytales_ByTitle_Success() {
        // When & Then
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "토끼")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("토끼와 거북이"))
            .andExpect(jsonPath("$.content[0].userId").value(user.id))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.size").value(10))
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 내용으로 검색 성공")
    fun searchFairytales_ByContent_Success() {
        // When & Then
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "일곱")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("백설공주"))
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 부분 일치 검색 성공")
    fun searchFairytales_PartialMatch_Success() {
        // When & Then
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "공주")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(2)) // 백설공주, 신데렐라(왕자님)
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 페이징 기능 테스트")
    fun searchFairytales_Pagination_Success() {
        // When & Then - 첫 번째 페이지 (크기 2)
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "옛날")
                .param("page", "0")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1)) // "옛날"이 포함된 동화는 1개
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.number").value(0))

        // When & Then - 두 번째 페이지
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "옛날")
                .param("page", "1")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(0)) // 두 번째 페이지는 비어있음
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 정렬 기능 테스트 (최신순)")
    fun searchFairytales_SortByDate_Success() {
        // When & Then
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "살았") // "살았습니다"가 포함된 동화들
                .param("sortBy", "date")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            // 최신순이므로 가장 마지막에 생성된 신데렐라가 첫 번째
            .andExpect(jsonPath("$.content[0].title").value("백설공주"))
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 검색 결과 없음")
    fun searchFairytales_NoResults_EmptyPage() {
        // When & Then
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "존재하지않는키워드")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isEmpty)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 빈 키워드로 검색 실패")
    fun searchFairytales_EmptyKeyword_BadRequest() {
        // When & Then
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "   ")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 키워드 누락 실패")
    fun searchFairytales_MissingKeyword_BadRequest() {
        // When & Then
        mockMvc.perform(
            get("/api/fairytales/search")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 짧은 키워드 (1글자) 실패")
    fun searchFairytales_ShortKeyword_BadRequest() {
        // When & Then
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "토")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 페이지 파라미터 검증")
    fun searchFairytales_PageParameterValidation() {
        // When & Then - 음수 페이지는 0으로 변환
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "토끼")
                .param("page", "-1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.number").value(0))

        // When & Then - 큰 사이즈는 100으로 제한
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "토끼")
                .param("size", "150")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size").value(100))
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 다양한 정렬 옵션 테스트")
    fun searchFairytales_VariousSortOptions() {
        // When & Then - 제목 정렬
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "공주")
                .param("sortBy", "title")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)

        // When & Then - 알 수 없는 정렬 옵션 (기본값 사용)
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "공주")
                .param("sortBy", "unknown")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 한글과 영문 키워드 혼합 검색")
    fun searchFairytales_MixedKoreanEnglish() {
        // Given - 한영 혼합 제목 동화 추가
        val mixedFairytale = Fairytale(
            user = user,
            title = "Princess 백설공주 Story",
            content = "한영 혼합 내용입니다.",
            imageUrl = "https://example.com/mixed.jpg"
        )
        fairytaleRepository.save(mixedFairytale)

        // When & Then - 영문 키워드로 검색
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "Princess")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Princess 백설공주 Story"))

        // When & Then - 한글 키워드로 검색
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "백설")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2)) // 기존 백설공주 + 혼합 제목
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 공백이 포함된 키워드 검색")
    fun searchFairytales_KeywordWithSpaces() {
        // When & Then - 앞뒤 공백이 있는 키워드
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "  토끼  ")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("토끼와 거북이"))
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 특수문자가 포함된 키워드 검색")
    fun searchFairytales_KeywordWithSpecialCharacters() {
        // Given - 특수문자가 포함된 제목 동화 추가
        val specialFairytale = Fairytale(
            user = user,
            title = "행복한 동화!@#",
            content = "특수문자가 포함된 내용입니다.",
            imageUrl = "https://example.com/special.jpg"
        )
        fairytaleRepository.save(specialFairytale)

        // When & Then
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "!@#")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("행복한 동화!@#"))
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 범위별 검색 파라미터")
    fun searchFairytales_ScopeParameter() {
        // When & Then - all 범위
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "토끼")
                .param("scope", "all")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))

        // When & Then - title 범위 (구현되지 않은 경우에도 요청은 성공해야 함)
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "토끼")
                .param("scope", "title")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("GET /api/fairytales/search - 성능 시나리오 (다수의 동화)")
    fun searchFairytales_PerformanceScenario() {
        // Given - 추가 동화들 생성 (성능 테스트용)
        val fairytales = mutableListOf<Fairytale>()
        for (i in 1..20) {
            fairytales.add(
                Fairytale(
                    user = user,
                    title = "테스트 동화 $i",
                    content = "테스트 내용 $i - 토끼가 등장하는 동화입니다.",
                    imageUrl = "https://example.com/test$i.jpg"
                )
            )
        }
        fairytaleRepository.saveAll(fairytales)

        // When & Then - 검색 성능 확인
        mockMvc.perform(
            get("/api/fairytales/search")
                .param("keyword", "테스트")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(10))
            .andExpect(jsonPath("$.totalElements").value(20))
            .andExpect(jsonPath("$.totalPages").value(2))
    }
}