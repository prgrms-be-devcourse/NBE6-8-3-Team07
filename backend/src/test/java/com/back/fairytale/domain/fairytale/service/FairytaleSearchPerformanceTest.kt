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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.math.roundToInt

/**
 * 테스트 실행을 위해 fairytale 엔티티의 content 필드를 CLOB에서 VARCHAR(10000)로 변경필요
 */
@SpringBootTest(classes = [BackendApplication::class])
@ActiveProfiles("test")
@Transactional
class FairytaleSearchPerformanceTest {

    @Autowired
    private lateinit var fairytaleSearchService: FairytaleSearchService

    @Autowired
    private lateinit var fairytaleRepository: FairytaleRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    // Spring Boot Test에서 필요한 의존성 Mock
    @MockkBean
    private lateinit var geminiClient: GeminiClient

    @MockkBean
    private lateinit var huggingFaceClient: HuggingFaceClient

    @MockkBean
    private lateinit var googleCloudStorage: GoogleCloudStorage

    @MockkBean
    private lateinit var storage: Storage

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // 기존 데이터 정리
        fairytaleRepository.deleteAll()
        userRepository.deleteAll()

        // 성능 최적화 인덱스는 src/test/resources/schema.sql에서 자동 적용됨

        // 테스트용 사용자 생성
        testUser = User(
            email = "performance@test.com",
            name = "성능테스트",
            nickname = "퍼포먼스",
            socialId = "perf123"
        )
        userRepository.save(testUser)
    }

    @Test
    @DisplayName("검색 성능 테스트 - 1,000건 데이터")
    fun `검색 성능 측정 테스트_1000건`() {
        // 1. 테스트 데이터 생성
        val dataSize = 1000
        println("\n=== 검색 성능 테스트 시작 ===")
        println("테스트 데이터 생성 중... ($dataSize 건)")
        
        val createStartTime = System.currentTimeMillis()
        createTestData(dataSize)
        val createEndTime = System.currentTimeMillis()
        
        println("데이터 생성 완료: ${createEndTime - createStartTime}ms")
        
        // 2. 검색 성능 측정
        performSearchPerformanceTest()
    }

    @Test
    @DisplayName("검색 성능 테스트 - 10,000건 데이터")
    fun `검색 성능 측정 테스트_10000건`() {
        // 1. 테스트 데이터 생성
        val dataSize = 10000
        println("\n=== 대용량 검색 성능 테스트 시작 ===")
        println("테스트 데이터 생성 중... ($dataSize 건)")
        
        val createStartTime = System.currentTimeMillis()
        createTestData(dataSize)
        val createEndTime = System.currentTimeMillis()
        
        println("데이터 생성 완료: ${createEndTime - createStartTime}ms")
        
        // 2. 검색 성능 측정
        performSearchPerformanceTest()
    }

    @Test
    @DisplayName("검색 성능 테스트 - 80,000건 데이터")
    fun `검색 성능 측정 테스트_80000건`() {
        // 1. 테스트 데이터 생성
        val dataSize = 80000
        println("\n=== 초대용량 검색 성능 테스트 시작 ===")
        println("테스트 데이터 생성 중... ($dataSize 건)")
        println("시간이 오래 걸릴 수 있습니다...")
        
        val createStartTime = System.currentTimeMillis()
        createTestDataOptimized(dataSize)
        val createEndTime = System.currentTimeMillis()
        
        println("데이터 생성 완료: ${createEndTime - createStartTime}ms")
        
        // 2. 검색 성능 측정 (대용량 성능 테스트이므로 더 적은 반복으로 테스트)
        performSearchPerformanceTestOptimized()
    }

    private fun createTestData(count: Int) {
        val titles = listOf(
            "백설공주", "신데렐라", "잠자는 숲속의 공주", "라푼젤", "인어공주",
            "토끼와 거북이", "개미와 베짱이", "늑대와 일곱 마리 새끼 염소",
            "빨간 모자", "헨젤과 그레텔", "콩쥐팥쥐", "심청전", "춘향전",
            "흥부놀부", "견우직녀", "선녀와 나무꾼", "단군신화", "주몽전설"
        )
        
        val contentTemplates = listOf(
            "옛날 옛적에 %s가 살았습니다. 그들은 행복하게 살았어요.",
            "아름다운 %s 이야기입니다. 많은 모험을 겪었습니다.",
            "용감한 %s의 전설을 들려드릴게요. 감동적인 결말이 있습니다.",
            "신비로운 %s 왕국의 이야기예요. 마법과 사랑이 가득합니다.",
            "지혜로운 %s의 가르침을 담은 동화입니다. 교훈이 있어요."
        )

        val fairytales = mutableListOf<Fairytale>()
        
        for (i in 1..count) {
            val title = titles.random()
            val baseTitle = if (i > titles.size) "$title $i" else title
            val content = contentTemplates.random().format(baseTitle)
            
            val fairytale = Fairytale(
                user = testUser,
                title = baseTitle,
                content = content + " 이것은 성능 테스트를 위한 데이터입니다. 검색 기능의 속도를 측정하기 위해 생성되었습니다.",
                imageUrl = "https://example.com/test-$i.jpg"
            )
            
            fairytales.add(fairytale)
            
            // 배치 저장 (메모리 효율성을 위해)
            if (fairytales.size == 100) {
                fairytaleRepository.saveAll(fairytales)
                fairytales.clear()
            }
        }
        
        // 나머지 데이터 저장
        if (fairytales.isNotEmpty()) {
            fairytaleRepository.saveAll(fairytales)
        }
    }

    private fun createTestDataOptimized(count: Int) {
        val titles = listOf(
            "백설공주", "신데렐라", "잠자는 숲속의 공주", "라푼젤", "인어공주",
            "토끼와 거북이", "개미와 베짱이", "늑대와 일곱 마리 새끼 염소",
            "빨간 모자", "헨젤과 그레텔", "콩쥐팥쥐", "심청전", "춘향전",
            "흥부놀부", "견우직녀", "선녀와 나무꾼", "단군신화", "주몽전설"
        )
        
        val contentTemplates = listOf(
            "옛날 옛적에 %s가 살았습니다. 그들은 행복하게 살았어요.",
            "아름다운 %s 이야기입니다. 많은 모험을 겪었습니다.",
            "용감한 %s의 전설을 들려드릴게요. 감동적인 결말이 있습니다.",
            "신비로운 %s 왕국의 이야기예요. 마법과 사랑이 가득합니다.",
            "지혜로운 %s의 가르침을 담은 동화입니다. 교훈이 있어요."
        )

        val batchSize = 1000 // 대용량 데이터용 배치 크기 증가
        val fairytales = mutableListOf<Fairytale>()
        
        println("진행상황:")
        for (i in 1..count) {
            val title = titles.random()
            val baseTitle = if (i > titles.size) "$title $i" else title
            val content = contentTemplates.random().format(baseTitle)
            
            val fairytale = Fairytale(
                user = testUser,
                title = baseTitle,
                content = content + " 이것은 대용량 성능 테스트를 위한 데이터입니다. 검색 기능의 확장성을 측정하기 위해 생성되었습니다.",
                imageUrl = "https://example.com/test-$i.jpg"
            )
            
            fairytales.add(fairytale)
            
            // 배치 저장 및 진행상황 출력
            if (fairytales.size == batchSize) {
                fairytaleRepository.saveAll(fairytales)
                fairytales.clear()
                val progress = (i.toDouble() / count * 100).toInt()
                print("\r진행률: $progress% ($i/$count)")
            }
        }
        
        // 나머지 데이터 저장
        if (fairytales.isNotEmpty()) {
            fairytaleRepository.saveAll(fairytales)
        }
        println("\n데이터 생성 완료!")
    }

    private fun performSearchPerformanceTest() {
        val searchKeywords = listOf(
            "공주", "왕자", "옛날", "이야기", "동화", "사랑", "모험", "마법",
            "테스트", "성능", "검색", "데이터", "측정"
        )
        
        val results = mutableListOf<SearchResult>()
        val testCount = 50 // 각 키워드당 테스트 횟수
        
        println("\n=== 검색 성능 측정 시작 ===")
        println("검색어별로 $testCount 회씩 테스트")
        
        for (keyword in searchKeywords) {
            println("키워드 '$keyword' 테스트 중...")
            val keywordResults = mutableListOf<Long>()
            
            repeat(testCount) {
                val searchRequest = FairytaleSearchRequest(
                    keyword = keyword,
                    page = 0,
                    size = 10,
                    sortBy = "date",
                    scope = "all"
                )
                
                val startTime = System.nanoTime()
                fairytaleSearchService.search(searchRequest)
                val endTime = System.nanoTime()
                
                val duration = (endTime - startTime) / 1_000_000 // 나노초를 밀리초로 변환
                keywordResults.add(duration)
            }
            
            results.add(SearchResult(keyword, keywordResults))
        }
        
        // 결과 분석 및 출력
        printPerformanceResults(results)
    }

    private fun performSearchPerformanceTestOptimized() {
        val searchKeywords = listOf(
            "공주", "왕자", "옛날", "이야기", "동화", "사랑", "모험", "마법",
            "테스트", "성능", "검색", "데이터", "측정"
        )
        
        val results = mutableListOf<SearchResult>()
        val testCount = 20 // 대용량 데이터에서는 테스트 횟수 감소
        
        println("\n=== 대용량 검색 성능 측정 시작 ===")
        println("검색어별로 $testCount 회씩 테스트 (대용량 최적화)")
        
        for (keyword in searchKeywords) {
            println("키워드 '$keyword' 테스트 중...")
            val keywordResults = mutableListOf<Long>()
            
            repeat(testCount) {
                val searchRequest = FairytaleSearchRequest(
                    keyword = keyword,
                    page = 0,
                    size = 10,
                    sortBy = "date",
                    scope = "all"
                )
                
                val startTime = System.nanoTime()
                fairytaleSearchService.search(searchRequest)
                val endTime = System.nanoTime()
                
                val duration = (endTime - startTime) / 1_000_000 // 나노초를 밀리초로 변환
                keywordResults.add(duration)
            }
            
            results.add(SearchResult(keyword, keywordResults))
        }
        
        // 결과 분석 및 출력
        printPerformanceResults(results)
    }

    private fun printPerformanceResults(results: List<SearchResult>) {
        println("\n" + "=".repeat(60))
        println("검색 성능 테스트 결과")
        println("=".repeat(60))
        
        val allDurations = results.flatMap { it.durations }
        
        // 전체 통계
        val avgTime = allDurations.average()
        val minTime = allDurations.minOrNull() ?: 0L
        val maxTime = allDurations.maxOrNull() ?: 0L
        val p95Time = calculatePercentile(allDurations, 95.0)
        val p99Time = calculatePercentile(allDurations, 99.0)
        val tps = 1000.0 / avgTime
        
        println("전체 성능 지표:")
        println("  총 검색 횟수: ${allDurations.size}")
        println("  평균 응답시간: ${avgTime.roundToInt()}ms")
        println("  최소 응답시간: ${minTime}ms")
        println("  최대 응답시간: ${maxTime}ms")
        println("  P95 응답시간: ${p95Time.roundToInt()}ms")
        println("  P99 응답시간: ${p99Time.roundToInt()}ms")
        println("  처리량(TPS): ${"%.2f".format(tps)}")
        
        println("\n키워드별 상세 결과:")
        results.forEach { result ->
            val avg = result.durations.average()
            val min = result.durations.minOrNull() ?: 0L
            val max = result.durations.maxOrNull() ?: 0L
            
            println("  '${result.keyword}': 평균 ${avg.roundToInt()}ms (${min}~${max}ms)")
        }
        
        println("=".repeat(60))
    }

    private fun calculatePercentile(durations: List<Long>, percentile: Double): Double {
        val sorted = durations.sorted()
        val index = (percentile / 100.0 * (sorted.size - 1)).toInt()
        return sorted[index].toDouble()
    }

    data class SearchResult(
        val keyword: String,
        val durations: List<Long>
    )
}