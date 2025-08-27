package com.back.fairytale.global.security.oauth2;

import com.back.fairytale.global.security.CustomOAuth2User;
import com.back.fairytale.global.security.port.OAuth2UserManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final OAuth2UserManager oAuth2UserManager;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("oAuth2User = {}", oAuth2User);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        log.info("oAuth2User.getAttributes() = {}", response);

        OAuth2UserInfo userInfo = oAuth2UserManager.saveOrUpdateUser(response);
        return new CustomOAuth2User(userInfo.getId(), userInfo.getSocialId(), userInfo.getRole());
    }
}
