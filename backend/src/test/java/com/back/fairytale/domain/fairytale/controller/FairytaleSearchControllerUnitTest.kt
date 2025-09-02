package com.back.fairytale.domain.fairytale.controller

import com.back.fairytale.domain.fairytale.dto.search.FairytaleSearchRequest
import com.back.fairytale.domain.fairytale.dto.search.FairytaleSearchResponse
import com.back.fairytale.domain.fairytale.service.FairytaleSearchService
import com.back.fairytale.global.security.SecurityConfig
import com.back.fairytale.global.security.jwt.JwtAuthenticationFilter
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(
    controllers = [FairytaleSearchController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [SecurityConfig::class, JwtAuthenticationFilter::class]
        )
    ]
)
@AutoConfigureMockMvc(addFilters = false)
class FairytaleSearchControllerUnitTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var fairytaleSearchService: FairytaleSearchService

    private lateinit var mockSearchResponse: FairytaleSearchResponse

    @BeforeEach
    fun setUp() {
        mockSearchResponse = FairytaleSearchResponse(
            id = 1L,
            title = "토끼와 거북이",
            content = "옛날 옛적에 토끼와 거북이가 살았습니다. 토끼는 빨랐지만 게을렀고, 거북이는 느렸지만 꾸준했습니다.",
            imageUrl = "https://example.com/rabbit-turtle.jpg",
            childName = "민수",
            childRole = "토끼",
            characters = "토끼, 거북이",
            place = "숲",
            lesson = "꾸준함의 중요성",
            mood = "교훈적",
            userId = 1L,
            createdAt = LocalDateTime.now()
        )
    }

    @Nested
    @DisplayName("동화 검색 API")
    inner class SearchFairytalesApi {
        
        @Test
        @DisplayName("GET /api/fairytales/search - 기본 검색 성공")
        fun searchFairytales_BasicSearch_Success() {
            // Given
            val keyword = "토끼"
            val pageable = PageRequest.of(0, 10)
            val mockPage = PageImpl(listOf(mockSearchResponse), pageable, 1)
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } returns mockPage

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("토끼와 거북이"))
                .andExpect(jsonPath("$.content[0].characters").value("토끼, 거북이"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10))

            verify { fairytaleSearchService.search(any<FairytaleSearchRequest>()) }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 빈 키워드 검색 실패")
        fun searchFairytales_EmptyKeyword_BadRequest() {
            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", "   ")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { fairytaleSearchService.search(any()) }
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

            verify(exactly = 0) { fairytaleSearchService.search(any()) }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 페이징 파라미터 검증 (음수 페이지는 0으로 변환)")
        fun searchFairytales_NegativePage_Corrected() {
            // Given
            val keyword = "토끼"
            val pageable = PageRequest.of(0, 5)
            val mockPage = PageImpl(listOf(mockSearchResponse), pageable, 1)
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } returns mockPage

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .param("page", "-1")
                    .param("size", "5")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)

            verify { 
                fairytaleSearchService.search(match { request ->
                    request.page == 0 && request.keyword == keyword && request.size == 5
                })
            }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 큰 사이즈는 100으로 제한")
        fun searchFairytales_LargeSize_Limited() {
            // Given
            val keyword = "동화"
            val pageable = PageRequest.of(0, 100)
            val mockPage = PageImpl(listOf(mockSearchResponse), pageable, 1)
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } returns mockPage

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .param("page", "0")
                    .param("size", "150")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)

            verify { 
                fairytaleSearchService.search(match { request ->
                    request.size == 100 && request.keyword == keyword
                })
            }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 정렬 옵션 검증 (date)")
        fun searchFairytales_SortByDate() {
            // Given
            val keyword = "동화"
            val pageable = PageRequest.of(0, 10)
            val mockPage = PageImpl(listOf(mockSearchResponse), pageable, 1)
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } returns mockPage

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .param("sortBy", "date")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].title").value("토끼와 거북이"))

            verify { 
                fairytaleSearchService.search(match { request ->
                    request.sortBy == "date" && request.keyword == keyword
                })
            }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 검색 범위 검증 (all)")
        fun searchFairytales_ScopeAll() {
            // Given
            val keyword = "거북이"
            val pageable = PageRequest.of(0, 10)
            val mockPage = PageImpl(listOf(mockSearchResponse), pageable, 1)
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } returns mockPage

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .param("scope", "all")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].characters").value("토끼, 거북이"))

            verify { 
                fairytaleSearchService.search(match { request ->
                    request.scope == "all" && request.keyword == keyword
                })
            }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 검색 결과 없음 (빈 페이지)")
        fun searchFairytales_NoResults_EmptyPage() {
            // Given
            val keyword = "존재하지않는키워드"
            val emptyPage = PageImpl<FairytaleSearchResponse>(emptyList())
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } returns emptyPage

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isEmpty)
                .andExpect(jsonPath("$.totalElements").value(0))

            verify { fairytaleSearchService.search(any<FairytaleSearchRequest>()) }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 한글 키워드 검색")
        fun searchFairytales_KoreanKeyword() {
            // Given
            val keyword = "토끼와거북이"
            val pageable = PageRequest.of(0, 10)
            val mockPage = PageImpl(listOf(mockSearchResponse), pageable, 1)
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } returns mockPage

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].title").value("토끼와 거북이"))

            verify { 
                fairytaleSearchService.search(match { request ->
                    request.keyword == keyword
                })
            }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 영문 키워드 검색")
        fun searchFairytales_EnglishKeyword() {
            // Given
            val keyword = "rabbit"
            val mockSearchResponseEn = mockSearchResponse.copy(
                title = "Rabbit and Turtle Story",
                content = "Once upon a time, there were a rabbit and a turtle."
            )
            val pageable = PageRequest.of(0, 10)
            val mockPage = PageImpl(listOf(mockSearchResponseEn), pageable, 1)
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } returns mockPage

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].title").value("Rabbit and Turtle Story"))

            verify { fairytaleSearchService.search(any<FairytaleSearchRequest>()) }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 서비스 예외 발생 시 500 에러")
        fun searchFairytales_ServiceException_InternalServerError() {
            // Given
            val keyword = "토끼"
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } throws RuntimeException("Database connection error")

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isInternalServerError)

            verify { fairytaleSearchService.search(any<FairytaleSearchRequest>()) }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 다중 검색 결과")
        fun searchFairytales_MultipleResults() {
            // Given
            val keyword = "동화"
            val searchResponse2 = mockSearchResponse.copy(
                id = 2L,
                title = "백설공주",
                content = "아름다운 백설공주의 이야기입니다.",
                characters = "백설공주, 일곱난쟁이",
                lesson = "선함의 승리"
            )
            val pageable = PageRequest.of(0, 10)
            val mockPage = PageImpl(listOf(mockSearchResponse, searchResponse2), pageable, 2)
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } returns mockPage

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("토끼와 거북이"))
                .andExpect(jsonPath("$.content[1].title").value("백설공주"))
                .andExpect(jsonPath("$.totalElements").value(2))

            verify { fairytaleSearchService.search(any<FairytaleSearchRequest>()) }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 제목 정렬 옵션")
        fun searchFairytales_SortByTitle() {
            // Given
            val keyword = "동화"
            val pageable = PageRequest.of(0, 10)
            val mockPage = PageImpl(listOf(mockSearchResponse), pageable, 1)
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } returns mockPage

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .param("sortBy", "title")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)

            verify { 
                fairytaleSearchService.search(match { request ->
                    request.sortBy == "title"
                })
            }
        }

        @Test
        @DisplayName("GET /api/fairytales/search - 페이지네이션 정보 확인")
        fun searchFairytales_PaginationInfo() {
            // Given
            val keyword = "토끼"
            val pageable = PageRequest.of(1, 5)
            val mockPage = PageImpl(listOf(mockSearchResponse), pageable, 10)
            every { fairytaleSearchService.search(any<FairytaleSearchRequest>()) } returns mockPage

            // When & Then
            mockMvc.perform(
                get("/api/fairytales/search")
                    .param("keyword", keyword)
                    .param("page", "1")
                    .param("size", "5")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))

            verify { fairytaleSearchService.search(any<FairytaleSearchRequest>()) }
        }
    }
}