package com.back.fairytale.domain.fairytale.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc


@WebMvcTest(FairytaleController::class)
class FairytaleControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var fairytaleService: FairytaleService

    private lateinit var mockCustomOAuth2User: CustomOAuth2User
    private lateinit var validRequest: FairytaleCreateRequest
    private lateinit var expectedResponse: FairytaleResponse

    @BeforeEach
    fun setUp() {
        // Mock CustomOAuth2User 설정
        mockCustomOAuth2User = mock(CustomOAuth2User::class.java)
        given(mockCustomOAuth2User.id).willReturn(1L)

        // 유효한 요청 데이터 설정
        validRequest = FairytaleCreateRequest(
            childName = "철수",
            childRole = "용감한 기사",
            characters = "마법사, 공주, 드래곤",
            place = "신비한 숲, 마법의 성",
            lesson = "용기, 우정",
            mood = "모험적인, 따뜻한"
        )

        // 예상 응답 데이터 설정
        expectedResponse = FairytaleResponse(
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
            userId = 1L,
            createdAt = LocalDateTime.of(2024, 1, 1, 10, 0)
        )
    }

    @Test
    @DisplayName("동화 생성 성공")
    @WithMockUser
    fun t1() {
        // given
        given(fairytaleService.createFairytale(any(FairytaleCreateRequest::class.java), eq(1L)))
            .willReturn(expectedResponse)

        // when & then
        val result = mockMvc.perform(
            post("/fairytales")
                .with(authentication(mockCustomOAuth2User))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()

        // 응답 검증
        val responseBody = result.response.contentAsString
        val actualResponse = objectMapper.readValue(responseBody, FairytaleResponse::class.java)

        assertThat(actualResponse).isNotNull
        assertThat(actualResponse.id).isEqualTo(expectedResponse.id)
        assertThat(actualResponse.title).isEqualTo(expectedResponse.title)
        assertThat(actualResponse.content).isEqualTo(expectedResponse.content)
        assertThat(actualResponse.imageUrl).isEqualTo(expectedResponse.imageUrl)
        assertThat(actualResponse.childName).isEqualTo(expectedResponse.childName)
        assertThat(actualResponse.childRole).isEqualTo(expectedResponse.childRole)
        assertThat(actualResponse.characters).isEqualTo(expectedResponse.characters)
        assertThat(actualResponse.place).isEqualTo(expectedResponse.place)
        assertThat(actualResponse.lesson).isEqualTo(expectedResponse.lesson)
        assertThat(actualResponse.mood).isEqualTo(expectedResponse.mood)
        assertThat(actualResponse.userId).isEqualTo(expectedResponse.userId)
        assertThat(actualResponse.createdAt).isEqualTo(expectedResponse.createdAt)

        // 서비스 메서드 호출 검증
        verify(fairytaleService, times(1)).createFairytale(any(FairytaleCreateRequest::class.java), eq(1L))
    }
}