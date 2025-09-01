package com.back.fairytale.domain.user.controller

import com.back.fairytale.domain.user.dto.TokenPairDto
import com.back.fairytale.domain.user.service.AuthService
import com.back.fairytale.global.security.CustomOAuth2User
import com.back.fairytale.global.security.SecurityConfig
import com.back.fairytale.global.security.jwt.JwtAuthenticationFilter
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@ActiveProfiles("test")
@WebMvcTest(
    controllers = [UserController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [SecurityConfig::class, JwtAuthenticationFilter::class]
        )
    ]
)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authService: AuthService

    private lateinit var validRefreshToken: String
    private lateinit var validAccessToken: String

    private lateinit var customOAuth2User: CustomOAuth2User

    @BeforeEach
    fun setUp() {
        validRefreshToken = "valid-refresh-token"
        validAccessToken = "valid-access-token"
        customOAuth2User = Mockito.mock(CustomOAuth2User::class.java)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("토큰 재발급이 성공적으로 이루어진다.")
    fun reissue_Success() {
        val tokenPairDto = TokenPairDto("newAccessToken", "newRefreshToken")
        val expectedAccessCookie = Cookie("Authorization", "newAccessToken").apply {
            isHttpOnly = true
            secure = true
            path = "/"
        }
        val expectedRefreshCookie = Cookie("refresh", "newRefreshToken").apply {
            isHttpOnly = true
            secure = true
            path = "/"
        }

        given(authService.getRefreshTokenFromCookies(any())).willReturn(validRefreshToken)
        given(authService.reissueTokens(validRefreshToken)).willReturn(tokenPairDto)
        given(authService.createAccessTokenCookie("newAccessToken")).willReturn(expectedAccessCookie)
        given(authService.createRefreshTokenCookie("newRefreshToken")).willReturn(expectedRefreshCookie)

        mockMvc.perform(
            post("/reissue")
                .cookie(Cookie("refresh", validRefreshToken))
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(cookie().exists("Authorization"))
            .andExpect(cookie().exists("refresh"))
            .andExpect(cookie().value("Authorization", "newAccessToken"))
            .andExpect(cookie().value("refresh", "newRefreshToken"))
            .andExpect(cookie().httpOnly("Authorization", true))
            .andExpect(cookie().httpOnly("refresh", true))

        verify(authService).getRefreshTokenFromCookies(any())
        verify(authService).reissueTokens(validRefreshToken)
        verify(authService).createAccessTokenCookie("newAccessToken")
        verify(authService).createRefreshTokenCookie("newRefreshToken")
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("잘못된 리프레시 토큰으로 재발급 시 실패한다.")
    fun reissue_InvalidRefreshToken_Failure() {
        val invalidRefreshToken = "invalid-refresh-token"
        given(authService.getRefreshTokenFromCookies(any())).willReturn(invalidRefreshToken)
        given(authService.reissueTokens(invalidRefreshToken))
            .willThrow(IllegalArgumentException("Refresh token이 유효하지 않습니다."))

        mockMvc.perform(
            post("/reissue")
                .cookie(Cookie("refresh", invalidRefreshToken))
                .with(csrf())
        )
            .andExpect(status().isBadRequest)

        verify(authService).getRefreshTokenFromCookies(any())
        verify(authService).reissueTokens(invalidRefreshToken)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("리프레시 토큰 쿠키가 없을 때 재발급 요청은 실패한다.")
    fun reissue_NoRefreshToken_Failure() {
        given(authService.getRefreshTokenFromCookies(any()))
            .willThrow(IllegalArgumentException("Refresh token이 유효하지 않습니다."))

        mockMvc.perform(
            post("/reissue")
                .with(csrf())
        )
            .andExpect(status().isBadRequest)

        verify(authService).getRefreshTokenFromCookies(any())
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("빈 리프레시 토큰으로 재발급 시 실패한다.")
    fun reissue_EmptyRefreshToken_Failure() {
        given(authService.getRefreshTokenFromCookies(any())).willReturn("")
        given(authService.reissueTokens(""))
            .willThrow(IllegalArgumentException("Refresh token이 유효하지 않습니다."))

        mockMvc.perform(
            post("/reissue")
                .with(csrf())
                .cookie(Cookie("refresh", ""))
        )
            .andExpect(status().isBadRequest)

        verify(authService).getRefreshTokenFromCookies(any())
        verify(authService).reissueTokens("")
    }

    @Test
    @DisplayName("인증된 사용자의 경우 성공적으로 접근이 가능하다.")
    fun getCurrentUser_Success() {
        mockMvc.perform(
            get("/users/me")
                .cookie(Cookie("Authorization", validAccessToken))
                .with(
                    authentication(
                        OAuth2AuthenticationToken(
                            customOAuth2User,
                            emptyList(),
                            "registrationId"
                        )
                    )
                )
        )
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("잘못된 토큰으로 사용자 정보 조회 시 401 에러가 발생한다.")
    fun getCurrentUser_InvalidToken() {
        val invalidToken = "invalid-access-token"

        mockMvc.perform(
            get("/users/me")
                .cookie(Cookie("Authorization", invalidToken))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("인증이 필요한 서비스입니다."))
    }
}
