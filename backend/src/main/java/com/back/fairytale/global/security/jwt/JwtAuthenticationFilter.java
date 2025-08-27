package com.back.fairytale.global.security.jwt;

import com.back.fairytale.domain.user.entity.User;
import com.back.fairytale.domain.user.repository.UserRepository;
import com.back.fairytale.global.security.CustomOAuth2User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static com.back.fairytale.global.security.jwt.JWTProvider.TokenType.ACCESS;
import static com.back.fairytale.global.security.jwt.JWTProvider.TokenType.REFRESH;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final JWTProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/h2-console") || request.getRequestURI().equals("/reissue");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = getTokenFromCookies(request, ACCESS.getName());
        if (isValidToken(accessToken)) {
            Long userId = jwtUtil.getUserId(accessToken);
            Optional<User> optionalUser = userRepository.findById(userId);

            if (optionalUser.isPresent()) {
                saveAuthenticate(optionalUser.get(), request, response, filterChain);
                return;
            }

            log.warn("유효한 토큰이나 사용자 없음. ID: {}", userId);
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = getTokenFromCookies(request, REFRESH.getName());
        if (!isValidToken(refreshToken)) {
            log.info("리프레시 토큰이 유효하지 않습니다.");
            filterChain.doFilter(request, response);
            return;
        }

        reissueTokens(refreshToken, request, response, filterChain);
    }

    private void reissueTokens(String refreshToken, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Long userId = jwtUtil.getUserId(refreshToken);
        Optional<User> optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        User user = optionalUser.get();
        if (!refreshToken.equals(user.getRefreshToken())) {
            log.info("이미 갱신된 토큰 요청. 사용자 ID: {}", userId);
            issueAccessToken(user, response);
            saveAuthenticate(user, request, response, filterChain);
            return;
        }

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().getKey());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getId(), user.getRole().getKey());

        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        response.addCookie(jwtProvider.wrapAccessTokenToCookie(newAccessToken));
        response.addCookie(jwtProvider.wrapRefreshTokenToCookie(newRefreshToken));

        log.info("토큰 재발급 완료. 사용자 ID: {}", userId);
        saveAuthenticate(user, request, response, filterChain);
    }

    private void saveAuthenticate(User user, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CustomOAuth2User principal = new CustomOAuth2User(user.getId(), user.getSocialId(), user.getRole().getKey());
        Authentication auth = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "naver");
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }

    private void issueAccessToken(User user, HttpServletResponse response) {
        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().getKey());
        response.addCookie(jwtProvider.wrapAccessTokenToCookie(newAccessToken));
        response.addCookie(jwtProvider.wrapRefreshTokenToCookie(user.getRefreshToken()));
    }

    private boolean isValidToken(String token) {
        return token != null && jwtUtil.validateToken(token);
    }

    private String getTokenFromCookies(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}