package com.back.fairytale.domain.bookmark.controller

import com.back.fairytale.domain.bookmark.dto.BookMarkDto
import com.back.fairytale.domain.bookmark.entity.BookMark
import com.back.fairytale.domain.bookmark.service.BookMarkService
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.enums.Role
import com.back.fairytale.global.security.CustomOAuth2User
import com.back.fairytale.global.security.SecurityConfig
import com.back.fairytale.global.security.jwt.JwtAuthenticationFilter
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@ActiveProfiles("test")
@WebMvcTest(
    controllers = [BookMarkController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [SecurityConfig::class, JwtAuthenticationFilter::class]
        )
    ]
)
class BookMarkControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var bookMarkService: BookMarkService

    private fun createCustomOAuth2User(): CustomOAuth2User =
        CustomOAuth2User(
            id = 1L,
            username = "test@naver.com",
            role = "ROLE_USER"
        )

    @Test
    @DisplayName("북마크 목록 조회")
    fun getBookMarks() {
        val oAuth2User = createCustomOAuth2User()
        val bookMarks = listOf(BookMarkDto(1L), BookMarkDto(2L))

        every { bookMarkService.getBookMark(oAuth2User.id) } returns bookMarks

        mockMvc.perform(
            get("/bookmarks")
                .with(oauth2Login().oauth2User(oAuth2User))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(bookMarks.size))
            .andExpect(jsonPath("$[0].fairytaleId").value(1L))

        verify { bookMarkService.getBookMark(oAuth2User.id) }
    }

    @Test
    @DisplayName("북마크 추가")
    fun addBookMark() {
        val oAuth2User = createCustomOAuth2User()
        val fairytaleId = 1L
        val user = User(id = 1L, email = "test@naver.com", nickname = "길동", role = Role.USER, socialId = "12345", name = "홍길동")
        val fairytale = Fairytale(title = "제목", content = "내용", user = user)
        val bookmark = BookMark(id = 1L, user = user, fairytale = fairytale)

        every { bookMarkService.addBookMark(fairytaleId, oAuth2User.id) } returns bookmark

        mockMvc.perform(
            post("/bookmark/{fairytaleId}", fairytaleId)
                .with(csrf())
                .with(oauth2Login().oauth2User(oAuth2User))
        )
            .andExpect(status().isCreated)
            .andExpect(content().string("게시물 1 즐겨찾기에 추가되었습니다."))

        verify { bookMarkService.addBookMark(fairytaleId, oAuth2User.id) }
    }

    @Test
    @DisplayName("북마크 삭제")
    fun removeBookMark() {
        val oAuth2User = createCustomOAuth2User()
        val fairytaleId = 1L

        every { bookMarkService.removeBookMark(oAuth2User.id, fairytaleId) } returns Unit

        mockMvc.perform(
            delete("/bookmark/{fairytaleId}", fairytaleId)
                .with(csrf())
                .with(oauth2Login().oauth2User(oAuth2User))
        )
            .andExpect(status().isOk)
            .andExpect(content().string("게시물 1 즐겨찾기에서 해제되었습니다."))

        verify { bookMarkService.removeBookMark(oAuth2User.id, fairytaleId) }
    }
}