package com.back.fairytale.global.security.port;

import com.back.fairytale.global.security.oauth2.OAuth2UserInfo;

import java.util.Map;

public interface OAuth2UserManager {
    OAuth2UserInfo saveOrUpdateUser(Map<String, Object> userInfo);
}
