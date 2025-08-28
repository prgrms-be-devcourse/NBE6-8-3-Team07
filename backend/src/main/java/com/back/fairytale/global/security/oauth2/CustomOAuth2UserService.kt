package com.back.fairytale.global.security.oauth2

import com.back.fairytale.global.security.CustomOAuth2User
import com.back.fairytale.global.security.port.OAuth2UserManager
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
class CustomOAuth2UserService(
    private val oAuth2UserManager: OAuth2UserManager
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest?): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)

        val attributes = oAuth2User.attributes
        val response = attributes["response"] as MutableMap<String, Any>

        val userInfo = oAuth2UserManager.saveOrUpdateUser(response)
        return CustomOAuth2User(userInfo.id, userInfo.socialId, userInfo.role)
    }
}
