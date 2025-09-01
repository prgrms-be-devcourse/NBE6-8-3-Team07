package com.back.fairytale.domain.keyword.controller

import com.back.fairytale.domain.keyword.dto.KeywordResponseDto
import com.back.fairytale.domain.keyword.enums.KeywordType
import com.back.fairytale.domain.keyword.service.KeywordService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.given
import org.mockito.quality.Strictness
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension::class)
class KeywordControllerTest {

    @Mock
    private lateinit var keywordService: KeywordService

    @InjectMocks
    private lateinit var keywordController: KeywordController

    private lateinit var mockMvc: MockMvc

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(keywordController).build()
    }

    @Test
    @DisplayName("키워드 전체 조회 API - 성공")
    fun getAllKeywords() {
        val responses = listOf(
            KeywordResponseDto(1L, "공주", KeywordType.CHARACTERS.name, 5),
            KeywordResponseDto(2L, "성", KeywordType.PLACE.name, 3)
        )
        given(keywordService.getAllKeywords()).willReturn(responses)

        mockMvc.perform(
            get("/api/keywords").contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
    }

    @Test
    @DisplayName("키워드 타입별 조회 API - 성공")
    fun getKeywordsByType() {
        val type = KeywordType.CHARACTERS
        val responses = listOf(
            KeywordResponseDto(1L, "공주", type.name, 5)
        )
        given(keywordService.getKeywordsByType(type)).willReturn(responses)

        mockMvc.perform(
            get("/api/keywords")
                .param("type", type.name)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
    }

    @Test
    @DisplayName("키워드 단건(특정) 조회 API - 성공")
    fun getKeywordById() {
        val id = 10L
        val response = KeywordResponseDto(id, "용", KeywordType.CHARACTERS.name, 7)
        given(keywordService.getKeywordById(id)).willReturn(response)

        mockMvc.perform(
            get("/api/keywords/$id").contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
    }
}
