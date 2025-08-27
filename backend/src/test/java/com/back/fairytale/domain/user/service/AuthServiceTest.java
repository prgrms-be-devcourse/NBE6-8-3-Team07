package com.back.fairytale.domain.user.service;

import com.back.fairytale.domain.user.dto.TokenPairDto;
import com.back.fairytale.domain.user.entity.User;
import com.back.fairytale.domain.user.enums.IsDeleted;
import com.back.fairytale.domain.user.enums.Role;
import com.back.fairytale.domain.user.repository.UserRepository;
import com.back.fairytale.global.security.jwt.JWTProvider;
import com.back.fairytale.global.util.impl.GoogleCloudStorage;
import com.google.cloud.storage.Storage;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTProvider jwtProvider;

    private User testUser;
    private String validRefreshToken;

    @MockitoBean
    private GoogleCloudStorage googleCloudStorage;

    @MockitoBean
    private Storage storage;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .name("테스트유저")
                .role(Role.USER)
                .socialId("testSocialId")
                .isDeleted(IsDeleted.NOT_DELETED)
                .build();

        User savedUser = userRepository.save(testUser);

        validRefreshToken = jwtProvider.createRefreshToken(savedUser.getId(), savedUser.getRole().getKey());
        savedUser.setRefreshToken(validRefreshToken);
    }

    @Test
    @DisplayName("토큰 재발급이 성공적으로 이루어진다.")
    void reissueToken() {
        // Given
        Cookie refreshTokenCookie = new Cookie("refresh", validRefreshToken);
        Cookie[] cookies = {refreshTokenCookie};

        // When
        String extractedRefreshToken = authService.getRefreshTokenFromCookies(cookies);
        TokenPairDto tokenPairDto = authService.reissueTokens(extractedRefreshToken);
        String newAccessToken = tokenPairDto.accessToken();
        String newRefreshToken = tokenPairDto.refreshToken();

        Cookie accessTokenCookie = authService.createAccessTokenCookie(newAccessToken);
        Cookie newRefreshTokenCookie = authService.createRefreshTokenCookie(newRefreshToken);

        // Then
        assertThat(jwtProvider.validateAccessToken(newAccessToken)).isTrue();
        assertThat(jwtProvider.validateRefreshToken(newRefreshToken)).isTrue();
        assertThat(newRefreshToken).isNotEqualTo(validRefreshToken);
        assertThat(accessTokenCookie.getValue()).isEqualTo(newAccessToken);
        assertThat(newRefreshTokenCookie.getValue()).isEqualTo(newRefreshToken);

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getRefreshToken()).isEqualTo(newRefreshToken);
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 재발급이 실패한다.")
    void reissueToken_InvalidToken() {
        // Given
        String invalidRefreshToken = "invalid.refresh.token";

        // When & Then
        assertThatThrownBy(() -> authService.reissueTokens(invalidRefreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token이 유효하지 않습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 재발급 시도 시 실패한다.")
    void reissueToken_NonExistentUser() {
        // Given
        Long nonExistentUserId = 999L;
        String tokenForNonExistentUser = jwtProvider.createRefreshToken(nonExistentUserId, "USER");

        // When & Then
        assertThatThrownBy(() -> authService.reissueTokens(tokenForNonExistentUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 유저가 존재하지 않습니다" );
    }

    @Test
    @DisplayName("리프레시 토큰이 다를 경우 재발급이 실패한다.")
    void reissueAccessToken_TokenMismatch() {
        // Given
        TokenPairDto tokenPairDto = authService.reissueTokens(validRefreshToken);

        // When & Then - 원래 토큰으로 재발급 시도
        assertThatThrownBy(() -> authService.reissueTokens(validRefreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh Token이 일치하지 않습니다.");
    }

    @Test
    @DisplayName("로그아웃이 성공적으로 이루어진다.")
    void logout() {
        // When
        authService.logout(testUser.getId());

        // Then
        User logOutUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(logOutUser.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자 로그아웃 시도 시 실패한다.")
    void logout_NonExistentUser() {
        // Given
        Long nonExistentUserId = 999L;

        // When & Then
        assertThatThrownBy(() -> authService.logout(nonExistentUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 유저가 존재하지 않습니다");
    }
}