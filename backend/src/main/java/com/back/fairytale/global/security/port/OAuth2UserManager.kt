package com.back.fairytale.global.security.port

import com.back.fairytale.global.security.oauth2.OAuth2UserInfo

interface OAuth2UserManager {
    fun saveOrUpdateUser(userInfo: MutableMap<String, Any>): OAuth2UserInfo
}
