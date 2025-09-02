package com.back.fairytale.domain.like.controller

import com.back.fairytale.domain.like.dto.LikeDto
import com.back.fairytale.domain.like.service.LikeService
import com.back.fairytale.global.security.CustomOAuth2User
import com.back.fairytale.global.security.SecurityConfig
import com.back.fairytale.global.security.jwt.JwtAuthenticationFilter
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@ActiveProfiles("test")
@WebMvcTest(
    controllers = [LikeController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [SecurityConfig::class, JwtAuthenticationFilter::class]
        )
    ]
)
class LikeControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var likeService: LikeService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val user = CustomOAuth2User(id = 1, username = "홍길동", role = "ROLE_USER")
    private val authorities = listOf(SimpleGrantedAuthority(user.role))
    private val authentication = OAuth2AuthenticationToken(user, authorities, "1234")

    @Test
    @DisplayName("좋아요 목록 조회")
    fun getLikes() {
        val likes = listOf(LikeDto(1), LikeDto(2))
        every { likeService.getLikes(any()) } returns likes

        mockMvc.perform(get("/likes").with(authentication(authentication)))
            .andExpect(status().isOk)
            .andExpect(content().json(objectMapper.writeValueAsString(likes)))
    }

    @Test
    @DisplayName("좋아요 추가")
    fun addLike() {
        every { likeService.addLike(any(), any()) } returns mockk()

        mockMvc.perform(
            post("/like/1")
                .with(authentication(authentication))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("게시물 1 좋아요가 추가되었습니다."))
    }

    @Test
    @DisplayName("좋아요 삭제")
    fun removeLike() {
        every { likeService.removeLike(any(), any()) } returns Unit

        mockMvc.perform(
            delete("/like/1")
                .with(authentication(authentication))
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(content().string("게시물 1 좋아요가 해제되었습니다."))
    }
}
