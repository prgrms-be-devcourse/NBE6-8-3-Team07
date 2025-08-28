package com.back.fairytale.global.security.oauth2;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuth2UserInfo {
    private Long id;
    private String socialId;
    private String name;
    private String nickname;
    private String email;
    private String role;
}
