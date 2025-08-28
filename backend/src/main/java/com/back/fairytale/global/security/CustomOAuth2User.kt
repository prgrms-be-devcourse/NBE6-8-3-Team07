package com.back.fairytale.global.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User
import java.util.Map

class CustomOAuth2User(
    val id: Long,
    val username: String,
    val role: String
) : OAuth2User {

    override fun getName(): String = username

    override fun getAttributes(): MutableMap<String, Any> = mutableMapOf()

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(GrantedAuthority { role })
    }
}
