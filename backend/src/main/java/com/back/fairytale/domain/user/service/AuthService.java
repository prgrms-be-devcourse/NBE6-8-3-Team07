package com.back.fairytale.domain.user.service;


import com.back.fairytale.domain.user.dto.TokenPairDto;
import com.back.fairytale.domain.user.entity.User;
import com.back.fairytale.domain.user.repository.UserRepository;
import com.back.fairytale.global.security.jwt.JWTProvider;
import com.back.fairytale.global.security.port.LogoutService;
import com.back.fairytale.global.security.port.UserTokenService;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class AuthService implements LogoutService, UserTokenService {

    private final JWTProvider jwtProvider;
    private final UserRepository userRepository;

    public String getRefreshTokenFromCookies(Cookie[] cookies) {
        return jwtProvider.extractRefreshToken(cookies);
    }

    public TokenPairDto reissueTokens(String refreshToken) {
        User user = validateRefreshTokenAndGetUser(refreshToken);

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().getKey());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getId(), user.getRole().getKey());

        user.setRefreshToken(newRefreshToken);

        return TokenPairDto.of(newAccessToken, newRefreshToken);
    }

    public Cookie createAccessTokenCookie(String token) {
        return jwtProvider.wrapAccessTokenToCookie(token);
    }

    public Cookie createRefreshTokenCookie(String refreshToken) {
        return jwtProvider.wrapRefreshTokenToCookie(refreshToken);
    }

    private User validateRefreshTokenAndGetUser(String refreshToken) {
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token이 유효하지 않습니다.");
        }

        Long userId = jwtProvider.getUserIdFromRefreshToken(refreshToken);

        User user = findUserById(userId);

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }

        return user;
    }

    @Override
    public void logout(Long userId) {
        User findUser = findUserById(userId);
        findUser.setRefreshToken(null);
    }

    @Override
    public String getUserToken(Long userId) {
        return findUserById(userId).getRefreshToken();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다. " + userId));
    }
}