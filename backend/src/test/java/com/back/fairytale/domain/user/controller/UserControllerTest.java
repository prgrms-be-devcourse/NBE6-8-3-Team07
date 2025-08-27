package com.back.fairytale.domain.user.controller;

import com.back.fairytale.domain.user.entity.User;
import com.back.fairytale.domain.user.enums.IsDeleted;
import com.back.fairytale.domain.user.enums.Role;
import com.back.fairytale.domain.user.repository.UserRepository;
import com.back.fairytale.global.security.jwt.JWTProvider;
import com.back.fairytale.global.util.impl.GoogleCloudStorage;
import com.google.cloud.storage.Storage;
import com.nimbusds.common.contenttype.ContentType;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JWTProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    private String validRefreshToken;
    private String validAccessToken;

    @MockitoBean
    private GoogleCloudStorage googleCloudStorage;

    @MockitoBean
    private Storage storage;

    @BeforeEach
    void setUp() {
        User testUser = User.builder()
                .email("test@example.com")
                .name("name")
                .role(Role.USER)
                .socialId("testSocialId")
                .isDeleted(IsDeleted.NOT_DELETED)
                .build();

        User savedUser = userRepository.save(testUser);
        validRefreshToken = jwtProvider.createRefreshToken(savedUser.getId(), savedUser.getRole().getKey());
        validAccessToken = jwtProvider.createAccessToken(savedUser.getId(), savedUser.getRole().getKey());

        savedUser.setRefreshToken(validRefreshToken);
    }

    @Test
    @DisplayName("토큰 재발급이 성공적으로 이루어진다.")
    void reissue_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/reissue")
                        .cookie(new Cookie("refresh", validRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("Authorization"))
                .andExpect(cookie().exists("refresh"));
    }

    @Test
    @DisplayName("잘못된 리프레시 토큰으로 재발급 시 실패한다.")
    void reissue_InvalidRefreshToken_Failure() throws Exception {
        // Given
        String invalidRefreshToken = "invalid-refresh-token";

        // When & Then
        mockMvc.perform(post("/reissue")
                        .cookie(new Cookie("refresh", invalidRefreshToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없을 때 재발급 요청은 실패한다.")
    void reissue_NoRefreshToken_Failure() throws Exception {
        // When & Then
        mockMvc.perform(post("/reissue"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("빈 리프레시 토큰으로 재발급 시 실패한다.")
    void reissue_EmptyRefreshToken_Failure() throws Exception {
        // When & Then
        mockMvc.perform(post("/reissue")
                        .cookie(new Cookie("refresh", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증된 사용자의 정보를 성공적으로 조회한다.")
    void getCurrentUser_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/me")
                        .cookie(new Cookie("Authorization", validAccessToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 사용자 정보 조회 시 401 에러가 발생한다.")
    void getCurrentUser_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(ContentType.APPLICATION_JSON.toString()))
                .andExpect(jsonPath("$.errorCode").value(401))
                .andExpect(jsonPath("$.message").value("인증이 필요한 서비스입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("잘못된 토큰으로 사용자 정보 조회 시 401 에러가 발생한다.")
    void getCurrentUser_InvalidToken() throws Exception {
        // Given
        String invalidToken = "invalid-access-token";

        // When & Then
        mockMvc.perform(get("/users/me")
                        .cookie(new Cookie("Authorization", invalidToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(ContentType.APPLICATION_JSON.toString()))
                .andExpect(jsonPath("$.errorCode").value(401))
                .andExpect(jsonPath("$.message").value("인증이 필요한 서비스입니다."))
                .andDo(print());
    }
}