package com.back.fairytale.domain.fairytale.service

import com.back.fairytale.domain.fairytale.dto.search.FairytaleSearchRequest
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.*
import org.mockito.quality.Strictness
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension::class)
class FairytaleSearchServiceUnitTest {

    @Mock
    private lateinit var fairytaleRepository: FairytaleRepository

    @InjectMocks
    private lateinit var fairytaleSearchService: FairytaleSearchService

    private fun createMockUser(id: Long, name: String, nickname: String): User {
        return mock {
            on { this.id } doReturn id
            on { this.name } doReturn name
            on { this.nickname } doReturn nickname
        }
    }

    private fun createMockFairytale(id: Long, title: String, content: String, user: User): Fairytale {
        return mock {
            on { this.id } doReturn id
            on { this.title } doReturn title
            on { this.content } doReturn content
            on { this.user } doReturn user
            on { this.createdAt } doReturn LocalDateTime.now()
            on { this.imageUrl } doReturn "https://example.com/image.jpg"
            on { this.getChildName() } doReturn "테스트 아이"
            on { this.getChildRole() } doReturn "주인공"
            on { this.getCharacters() } doReturn "토끼, 거북이"
            on { this.getPlace() } doReturn "숲속"
            on { this.getLesson() } doReturn "꾸준함의 중요성"
            on { this.getMood() } doReturn "교훈적"
        }
    }

    @Test
    @DisplayName("[성공] 키워드로 동화 검색 - 기본 검색")
    fun search_BasicKeyword_Success() {
        // Given
        val mockUser = createMockUser(1L, "홍길동", "길동")
        val mockFairytale = createMockFairytale(1L, "토끼와 거북이", "옛날 옛적에 토끼와 거북이가...", mockUser)
        val keyword = "토끼"
        val request = FairytaleSearchRequest(
            keyword = keyword,
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        val fairytalesPage = PageImpl(listOf(mockFairytale), pageable, 1)

        given(fairytaleRepository.searchByKeyword(keyword, pageable)).willReturn(fairytalesPage)

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].title).isEqualTo("토끼와 거북이")
        assertThat(result.content[0].characters).isEqualTo("토끼, 거북이")
        assertThat(result.totalElements).isEqualTo(1)
        
        verify(fairytaleRepository, times(1)).searchByKeyword(keyword, pageable)
    }

    @Test
    @DisplayName("[실패] 빈 키워드로 검색")
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

        verify(fairytaleRepository, never()).searchByKeyword(any(), any())
    }

    @Test
    @DisplayName("[실패] 짧은 키워드로 검색 (2글자 미만)")
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

        verify(fairytaleRepository, never()).searchByKeyword(any(), any())
    }

    @Test
    @DisplayName("[성공] 페이지 크기 검증 - 음수 페이지는 0으로 변경")
    fun search_NegativePage_CorrectedToZero() {
        // Given
        val mockUser = createMockUser(1L, "홍길동", "길동")
        val mockFairytale = createMockFairytale(1L, "토끼와 거북이", "내용", mockUser)
        val request = FairytaleSearchRequest(
            keyword = "토끼",
            page = -5,
            size = 10,
            sortBy = "date",
            scope = "all"
        )
        val expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        val fairytalesPage = PageImpl(listOf(mockFairytale), expectedPageable, 1)

        given(fairytaleRepository.searchByKeyword("토끼", expectedPageable)).willReturn(fairytalesPage)

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertThat(result.number).isEqualTo(0) // 페이지 번호가 0으로 수정되었는지 확인
        verify(fairytaleRepository).searchByKeyword("토끼", expectedPageable)
    }

    @Test
    @DisplayName("[성공] 페이지 크기 검증 - 큰 사이즈는 100으로 제한")
    fun search_LargeSize_LimitedToHundred() {
        // Given
        val mockUser = createMockUser(1L, "홍길동", "길동")
        val mockFairytale = createMockFairytale(1L, "토끼와 거북이", "내용", mockUser)
        val request = FairytaleSearchRequest(
            keyword = "토끼",
            page = 0,
            size = 150,
            sortBy = "date",
            scope = "all"
        )
        val expectedPageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"))
        val fairytalesPage = PageImpl(listOf(mockFairytale), expectedPageable, 1)

        given(fairytaleRepository.searchByKeyword("토끼", expectedPageable)).willReturn(fairytalesPage)

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertThat(result.size).isEqualTo(100) // 사이즈가 100으로 제한되었는지 확인
        verify(fairytaleRepository).searchByKeyword("토끼", expectedPageable)
    }

    @Test
    @DisplayName("[성공] 정렬 옵션 검증 - date 정렬")
    fun search_SortByDate_Success() {
        // Given
        val mockUser = createMockUser(1L, "홍길동", "길동")
        val mockFairytale = createMockFairytale(1L, "최신 동화", "내용", mockUser)
        val request = FairytaleSearchRequest(
            keyword = "동화",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )
        val expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        val fairytalesPage = PageImpl(listOf(mockFairytale), expectedPageable, 1)

        given(fairytaleRepository.searchByKeyword("동화", expectedPageable)).willReturn(fairytalesPage)

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertThat(result.content[0].title).isEqualTo("최신 동화")
        verify(fairytaleRepository).searchByKeyword("동화", expectedPageable)
    }

    @Test
    @DisplayName("[성공] 정렬 옵션 검증 - title 정렬")
    fun search_SortByTitle_Success() {
        // Given
        val mockUser = createMockUser(1L, "홍길동", "길동")
        val mockFairytale = createMockFairytale(1L, "가나다 동화", "내용", mockUser)
        val request = FairytaleSearchRequest(
            keyword = "동화",
            page = 0,
            size = 10,
            sortBy = "title",
            scope = "all"
        )
        val expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "title"))
        val fairytalesPage = PageImpl(listOf(mockFairytale), expectedPageable, 1)

        given(fairytaleRepository.searchByKeyword("동화", expectedPageable)).willReturn(fairytalesPage)

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertThat(result.content[0].title).isEqualTo("가나다 동화")
        verify(fairytaleRepository).searchByKeyword("동화", expectedPageable)
    }

    @Test
    @DisplayName("[성공] 알 수 없는 정렬 옵션 - 기본값(createdAt) 사용")
    fun search_UnknownSortBy_DefaultToCreatedAt() {
        // Given
        val mockUser = createMockUser(1L, "홍길동", "길동")
        val mockFairytale = createMockFairytale(1L, "동화", "내용", mockUser)
        val request = FairytaleSearchRequest(
            keyword = "동화",
            page = 0,
            size = 10,
            sortBy = "unknownSort",
            scope = "all"
        )
        val expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        val fairytalesPage = PageImpl(listOf(mockFairytale), expectedPageable, 1)

        given(fairytaleRepository.searchByKeyword("동화", expectedPageable)).willReturn(fairytalesPage)

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertThat(result.content).isNotEmpty
        verify(fairytaleRepository).searchByKeyword("동화", expectedPageable)
    }

    @Test
    @DisplayName("[성공] 검색 결과 없음")
    fun search_NoResults_EmptyPage() {
        // Given
        val request = FairytaleSearchRequest(
            keyword = "존재하지않는키워드",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        val emptyPage = PageImpl<Fairytale>(emptyList(), pageable, 0)

        given(fairytaleRepository.searchByKeyword("존재하지않는키워드", pageable)).willReturn(emptyPage)

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
        verify(fairytaleRepository).searchByKeyword("존재하지않는키워드", pageable)
    }

    @Test
    @DisplayName("[성공] 다중 검색 결과")
    fun search_MultipleResults_Success() {
        // Given
        val mockUser = createMockUser(1L, "홍길동", "길동")
        val fairytale1 = createMockFairytale(1L, "토끼와 거북이", "토끼 이야기", mockUser)
        val fairytale2 = createMockFairytale(2L, "토끼의 모험", "또 다른 토끼 이야기", mockUser)
        
        val request = FairytaleSearchRequest(
            keyword = "토끼",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        val fairytalesPage = PageImpl(listOf(fairytale1, fairytale2), pageable, 2)

        given(fairytaleRepository.searchByKeyword("토끼", pageable)).willReturn(fairytalesPage)

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertThat(result.content).hasSize(2)
        assertThat(result.content[0].title).isEqualTo("토끼와 거북이")
        assertThat(result.content[1].title).isEqualTo("토끼의 모험")
        assertThat(result.totalElements).isEqualTo(2)
        
        verify(fairytaleRepository).searchByKeyword("토끼", pageable)
    }

    @Test
    @DisplayName("[성공] 키워드 공백 제거")
    fun search_TrimKeyword_Success() {
        // Given
        val mockUser = createMockUser(1L, "홍길동", "길동")
        val mockFairytale = createMockFairytale(1L, "토끼와 거북이", "내용", mockUser)
        val request = FairytaleSearchRequest(
            keyword = "  토끼  ",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        val fairytalesPage = PageImpl(listOf(mockFairytale), pageable, 1)

        given(fairytaleRepository.searchByKeyword("토끼", pageable)).willReturn(fairytalesPage)

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertThat(result.content).hasSize(1)
        verify(fairytaleRepository).searchByKeyword("토끼", pageable) // 공백이 제거된 키워드로 호출되는지 확인
    }

    @Test
    @DisplayName("[성공] DTO 변환 검증")
    fun search_DtoConversion_Success() {
        // Given
        val mockUser = createMockUser(1L, "홍길동", "길동")
        val mockFairytale = createMockFairytale(1L, "토끼와 거북이", "토끼는 빨랐지만...", mockUser)
        val request = FairytaleSearchRequest(
            keyword = "토끼",
            page = 0,
            size = 10,
            sortBy = "date",
            scope = "all"
        )
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        val fairytalesPage = PageImpl(listOf(mockFairytale), pageable, 1)

        given(fairytaleRepository.searchByKeyword("토끼", pageable)).willReturn(fairytalesPage)

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        val searchResponse = result.content[0]
        assertThat(searchResponse.id).isEqualTo(1L)
        assertThat(searchResponse.title).isEqualTo("토끼와 거북이")
        assertThat(searchResponse.content).isEqualTo("토끼는 빨랐지만...")
        assertThat(searchResponse.userId).isEqualTo(1L)
        assertThat(searchResponse.childName).isEqualTo("테스트 아이")
        assertThat(searchResponse.characters).isEqualTo("토끼, 거북이")
        assertThat(searchResponse.place).isEqualTo("숲속")
        assertThat(searchResponse.lesson).isEqualTo("꾸준함의 중요성")
        assertThat(searchResponse.mood).isEqualTo("교훈적")
    }

    @Test
    @DisplayName("[성공] 대소문자 구분 없는 정렬 옵션")
    fun search_CaseInsensitiveSortBy_Success() {
        // Given
        val mockUser = createMockUser(1L, "홍길동", "길동")
        val mockFairytale = createMockFairytale(1L, "동화", "내용", mockUser)
        val request = FairytaleSearchRequest(
            keyword = "동화",
            page = 0,
            size = 10,
            sortBy = "LATEST", // 대문자로 입력
            scope = "all"
        )
        val expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        val fairytalesPage = PageImpl(listOf(mockFairytale), expectedPageable, 1)

        given(fairytaleRepository.searchByKeyword("동화", expectedPageable)).willReturn(fairytalesPage)

        // When
        val result = fairytaleSearchService.search(request)

        // Then
        assertThat(result.content).isNotEmpty
        verify(fairytaleRepository).searchByKeyword("동화", expectedPageable)
    }
}