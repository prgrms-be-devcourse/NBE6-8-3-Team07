package com.back.fairytale.domain.fairytale.service

import com.back.BackendApplication
import com.back.fairytale.domain.fairytale.dto.search.FairytaleSearchRequest
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import com.google.cloud.storage.Storage
import com.ninjasquad.springmockk.MockkBean
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [BackendApplication::class])
@ActiveProfiles("test")
@Transactional
class FairytaleSearchServiceIntegrationTest @Autowired constructor(
    private val fairytaleSearchService: FairytaleSearchService,
    private val fairytaleRepository: FairytaleRepository,
    private val userRepository: UserRepository
) {

    // Spring Boot Test(Spring Context)에서 MockkBean을 사용하여 필요한 의존성 주입
    @MockkBean
    private lateinit var geminiClient: GeminiClient

    @MockkBean
    private lateinit var huggingFaceClient: HuggingFaceClient

    @MockkBean
    private lateinit var googleCloudStorage: GoogleCloudStorage

    @MockkBean
    private lateinit var storage: Storage

    private lateinit var user: User
    private lateinit var fairytale1: Fairytale
    private lateinit var fairytale2: Fairytale
    private lateinit var fairytale3: Fairytale

    @BeforeEach
    fun setUp() {
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
            content = "옛날 옛적에 토끼와 거북이가 살았습니다. 토끼는 빨랐지만 게을렀고, 거북이는 느렸지만 꾸준했습니다. 결국 거북이가 이겼습니다.",
            imageUrl = "https://example.com/rabbit-turtle.jpg"
        )
        fairytaleRepository.save(fairytale1)

        fairytale2 = Fairytale(
            user = user,
            title = "백설공주",
            content = "아름다운 백설공주가 일곱 난쟁이와 함께 살았습니다. 계모가 질투했지만 결국 왕자님을 만나 행복하게 살았습니다.",
            imageUrl = "https://example.com/snow-white.jpg"
        )
        fairytaleRepository.save(fairytale2)

        fairytale3 = Fairytale(
            user = user,
            title = "신데렐라",
            content = "착한 신데렐라는 계모와 언니들의 괴롭힘을 받았지만, 요정의 도움으로 왕자님을 만나 행복해졌습니다.",
            imageUrl = "https://example.com/cinderella.jpg"
        )
        fairytaleRepository.save(fairytale3)
    }

    @Test
    @DisplayName("제목으로 동화 검색 성공")
    fun search_ByTitle_Success() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "토끼",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertEquals(1, result.content.size)
        assertEquals("토끼와 거북이", result.content[0].title)
        assertEquals(user.id, result.content[0].userId)
        assertNotNull(result.content[0].createdAt)
    }

    @Test
    @DisplayName("내용으로 동화 검색 성공")
    fun search_ByContent_Success() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "일곱",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertEquals(1, result.content.size)
        assertEquals("백설공주", result.content[0].title)
        assertTrue(result.content[0].content.contains("일곱"))
    }

    @Test
    @DisplayName("부분 일치 검색 성공")
    fun search_PartialMatch_Success() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "왕자",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertEquals(2, result.content.size) // 백설공주, 신데렐라
        assertTrue(result.content.any { it.title == "백설공주" })
        assertTrue(result.content.any { it.title == "신데렐라" })
    }

    @Test
    @DisplayName("대소문자 구분 없는 검색 성공")
    fun search_CaseInsensitive_Success() {
        // Given - 영문 동화 추가
        val englishFairytale = Fairytale(
            user = user,
            title = "Little Red Riding Hood",
            content = "A Little girl went to visit her Grandmother.",
            imageUrl = "https://example.com/red-hood.jpg"
        )
        fairytaleRepository.save(englishFairytale)

        val requestLower = FairytaleSearchRequest(
            keyword = "little",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        val requestUpper = FairytaleSearchRequest(
            keyword = "LITTLE",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When
        val resultLower = fairytaleSearchService.search(requestLower)
        val resultUpper = fairytaleSearchService.search(requestUpper)

        // Then
        assertEquals(1, resultLower.content.size)
        assertEquals(1, resultUpper.content.size)
        assertEquals("Little Red Riding Hood", resultLower.content[0].title)
        assertEquals("Little Red Riding Hood", resultUpper.content[0].title)
    }

    @Test
    @DisplayName("빈 키워드로 검색 실패")
    fun search_EmptyKeyword_ThrowsException() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "   ",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When & Then
        assertThrows<IllegalArgumentException> {
            fairytaleSearchService.search(request)
        }
    }

    @Test
    @DisplayName("짧은 키워드(1글자)로 검색 실패")
    fun search_ShortKeyword_ThrowsException() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "토",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When & Then
        assertThrows<IllegalArgumentException> {
            fairytaleSearchService.search(request)
        }
    }

    @Test
    @DisplayName("페이징 기능 테스트")
    fun search_Pagination_Success() {
        // Given - 추가 동화 생성
        for (i in 1..5) {
            val fairytale = Fairytale(
                user = user,
                title = "테스트 동화 $i",
                content = "테스트 내용 $i",
                imageUrl = "https://example.com/test$i.jpg"
            )
            fairytaleRepository.save(fairytale)
        }

        val request = FairytaleSearchRequest(
            keyword = "테스트",
            page = 0,
            size = 3,
            sortBy = "date",
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertEquals(3, result.content.size)
        assertEquals(5, result.totalElements)
        assertEquals(2, result.totalPages)
        assertEquals(0, result.number)
    }

    @Test
    @DisplayName("정렬 기능 테스트 - 날짜순")
    fun search_SortByDate_Success() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "왕자",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertEquals(2, result.content.size)
        // 최신순 정렬이므로 나중에 생성된 신데렐라가 먼저 나와야 함
        assertEquals("신데렐라", result.content[0].title)
        assertEquals("백설공주", result.content[1].title)
    }

    @Test
    @DisplayName("정렬 기능 테스트 - 제목순")
    fun search_SortByTitle_Success() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "왕자",
            page = 0,
            size = 10,
            sortBy = "title",
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertEquals(2, result.content.size)
        // 제목 역순 정렬이므로 '신' > '백'
        assertEquals("신데렐라", result.content[0].title)
        assertEquals("백설공주", result.content[1].title)
    }

    @Test
    @DisplayName("알 수 없는 정렬 옵션 - 기본값(날짜순) 사용")
    fun search_UnknownSortOption_DefaultToDate() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "토끼",
            page = 0,
            size = 10,
            sortBy = "unknownSort",
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertEquals(1, result.content.size)
        assertEquals("토끼와 거북이", result.content[0].title)
    }

    @Test
    @DisplayName("검색 결과 없음")
    fun search_NoResults_EmptyPage() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "존재하지않는키워드",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertTrue(result.content.isEmpty())
        assertEquals(0, result.totalElements)
    }

    @Test
    @DisplayName("페이지 파라미터 검증")
    fun search_PageParameterValidation() {
        // Given - 음수 페이지
        val requestNegativePage = FairytaleSearchRequest(
            keyword = "토끼",
            page = -5,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When
        val resultNegativePage = fairytaleSearchService.search(requestNegativePage)

        // Then - 페이지가 0으로 수정되어야 함
        assertEquals(0, resultNegativePage.number)

        // Given - 큰 사이즈
        val requestLargeSize = FairytaleSearchRequest(
            keyword = "토끼",
            page = 0,
            size = 150,
            sortBy = "date",
            scope = "all"
        )

        // When
        val resultLargeSize = fairytaleSearchService.search(requestLargeSize)

        // Then - 사이즈가 100으로 제한되어야 함
        assertEquals(100, resultLargeSize.size)
    }

    @Test
    @DisplayName("키워드 공백 제거 테스트")
    fun search_TrimKeyword_Success() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "  토끼  ",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertEquals(1, result.content.size)
        assertEquals("토끼와 거북이", result.content[0].title)
    }

    @Test
    @DisplayName("특수문자가 포함된 키워드 검색")
    fun search_SpecialCharacters_Success() {
        // Given - 특수문자가 포함된 동화 추가
        val specialFairytale = Fairytale(
            user = user,
            title = "행복한 동화!@#",
            content = "특수문자 내용입니다.",
            imageUrl = "https://example.com/special.jpg"
        )
        fairytaleRepository.save(specialFairytale)

        val request = FairytaleSearchRequest(
            keyword = "!@#",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertEquals(1, result.content.size)
        assertEquals("행복한 동화!@#", result.content[0].title)
    }

    @Test
    @DisplayName("성능 시나리오 - 다수의 동화 검색")
    fun search_PerformanceScenario_Success() {
        // Given - 대량의 테스트 데이터 생성
        val fairytales = mutableListOf<Fairytale>()
        for (i in 1..100) {
            fairytales.add(
                Fairytale(
                    user = user,
                    title = "성능 테스트 동화 $i",
                    content = "토끼가 등장하는 성능 테스트용 내용 $i",
                    imageUrl = "https://example.com/perf$i.jpg"
                )
            )
        }
        fairytaleRepository.saveAll(fairytales)

        val request = FairytaleSearchRequest(
            keyword = "토끼",
            page = 0,
            size = 50,
            sortBy = "date",
            scope = "all"
        )

        // When
        val startTime = System.currentTimeMillis()
        val result = fairytaleSearchService.search(request)
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Then
        assertEquals(50, result.content.size) // 첫 페이지 50개
        assertEquals(101, result.totalElements) // 기존 1개 + 새로 추가된 100개
        assertTrue(duration < 2000) // 2초 이내 완료 (성능 기준)

        // 로그를 통해 성능 정보 확인 가능
        println("검색 성능 테스트 완료 - 소요시간: ${duration}ms, 결과수: ${result.totalElements}")
    }

    @Test
    @DisplayName("한영 혼합 키워드 검색")
    fun search_MixedKoreanEnglish_Success() {
        // Given - 한영 혼합 동화 추가
        val mixedFairytale = Fairytale(
            user = user,
            title = "Princess 백설공주 Story",
            content = "한영 혼합 동화입니다. Beautiful princess lived in the forest.",
            imageUrl = "https://example.com/mixed.jpg"
        )
        fairytaleRepository.save(mixedFairytale)

        // When - 영문 키워드 검색
        val englishRequest = FairytaleSearchRequest(
            keyword = "Princess",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )
        val englishResult = fairytaleSearchService.search(englishRequest)

        // When - 한글 키워드 검색
        val koreanRequest = FairytaleSearchRequest(
            keyword = "백설공주",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )
        val koreanResult = fairytaleSearchService.search(koreanRequest)

        // Then
        assertEquals(1, englishResult.content.size)
        assertEquals("Princess 백설공주 Story", englishResult.content[0].title)
        assertEquals(2, koreanResult.content.size) // 기존 백설공주 + 혼합 제목
    }

    @Test
    @DisplayName("정확한 키워드 매칭 확인")
    fun search_ExactMatching_Success() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "토끼와 거북이",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertEquals(1, result.content.size)
        assertEquals("토끼와 거북이", result.content[0].title)
    }

    @Test
    @DisplayName("대소문자 혼합 정렬 옵션")
    fun search_CaseInsensitiveSortBy_Success() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "토끼",
            page = 0,
            size = 10,
            sortBy = "LATEST", // 대문자로 입력
            scope = "all"
        )

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertEquals(1, result.content.size)
        assertEquals("토끼와 거북이", result.content[0].title)
    }
}