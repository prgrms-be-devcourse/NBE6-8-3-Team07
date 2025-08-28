package com.back.fairytale.global.security.oauth2;

import com.back.fairytale.global.security.port.LogoutService;
import com.back.fairytale.global.security.jwt.JWTProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {
    private final LogoutService logoutService;
    private final JWTProvider jwtProvider;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (request.getMethod().equals("GET")) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "GET method not allowed");
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            String accessToken = jwtProvider.extractTokenFromCookies(request.getCookies(), JWTProvider.TokenType.ACCESS);

            if (accessToken != null && jwtProvider.validateAccessToken(accessToken)) {
                Long userId = jwtProvider.getUserIdFromAccessToken(accessToken);
                logoutService.logout(userId);
            }

        } catch (Exception e) {
            log.warn("Logout failed: {}", e.getMessage());
        } finally {
            response.addCookie(jwtProvider.createCookie(null, "Authorization", 0));
            response.addCookie(jwtProvider.createCookie(null, "refresh", 0));
        }
    }
}
