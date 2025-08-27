package com.back.fairytale.domain.user.controller;

import com.back.fairytale.domain.user.dto.TokenPairDto;
import com.back.fairytale.domain.user.service.AuthService;
import com.back.fairytale.global.security.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final AuthService authService;

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = authService.getRefreshTokenFromCookies(request.getCookies());

            TokenPairDto tokenPairDto = authService.reissueTokens(refreshToken);

            response.addCookie(authService.createAccessTokenCookie(tokenPairDto.accessToken()));
            response.addCookie(authService.createRefreshTokenCookie(tokenPairDto.refreshToken()));
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Refresh token invalid: {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/users/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 정보가 없습니다.");
        }

        return ResponseEntity.ok(null);
    }
}