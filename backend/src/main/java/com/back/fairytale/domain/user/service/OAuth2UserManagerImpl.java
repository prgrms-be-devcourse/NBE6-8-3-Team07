package com.back.fairytale.domain.user.service;

import com.back.fairytale.domain.user.entity.User;
import com.back.fairytale.domain.user.enums.IsDeleted;
import com.back.fairytale.domain.user.enums.Role;
import com.back.fairytale.domain.user.repository.UserRepository;
import com.back.fairytale.global.security.jwt.JWTProvider;
import com.back.fairytale.global.security.oauth2.OAuth2UserInfo;
import com.back.fairytale.global.security.port.OAuth2UserManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OAuth2UserManagerImpl implements OAuth2UserManager {
    private final UserRepository userRepository;
    private final JWTProvider jwtProvider;

    @Override
    public OAuth2UserInfo saveOrUpdateUser(Map<String, Object> response) {
        User user = userRepository.findBySocialId(response.get("id").toString())
                .map(entity -> entity.update(
                        response.get("name").toString(),
                        response.get("nickname").toString(),
                        response.get("email").toString()))
                .orElse(createUser(response));

        User savedUser = userRepository.save(user);

        String newRefreshToken = jwtProvider.createRefreshToken(savedUser.getId(), savedUser.getRole().getKey());
        savedUser.setRefreshToken(newRefreshToken);

        return OAuth2UserInfo.builder()
                .id(savedUser.getId())
                .socialId(savedUser.getSocialId())
                .name(savedUser.getName())
                .nickname(savedUser.getNickname())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().getKey())
                .build();
    }

    private User createUser(Map<String, Object> response) {
        return User.builder()
                .socialId(response.get("id").toString())
                .name(response.get("name").toString())
                .nickname(response.get("nickname").toString())
                .email(response.get("email").toString())
                .role(Role.USER)
                .isDeleted(IsDeleted.NOT_DELETED)
                .build();
    }
}
