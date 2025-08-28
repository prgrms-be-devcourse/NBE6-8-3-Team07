package com.back.fairytale.global.security.oauth2;

import com.back.fairytale.global.security.CorsProperties;
import com.back.fairytale.global.security.CustomOAuth2User;
import com.back.fairytale.global.security.port.UserTokenService;
import com.back.fairytale.global.security.jwt.JWTProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JWTProvider jwtProvider;
    private final UserTokenService userTokenService;
    private final CorsProperties corsProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("OAuth2 로그인 성공: {}", authentication.getName());
        CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();

        Long userId = customUser.getId();
        String refreshToken = userTokenService.getUserToken(userId);

        Cookie refreshCookie = jwtProvider.createRefreshTokenCookie(refreshToken);

        String redirectUrl = corsProperties.getAllowedOrigins().getFirst();

        response.addCookie(refreshCookie);
        response.sendRedirect(redirectUrl);
    }
}
