package com.back.fairytale.domain.user.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Role {
    ADMIN("ROLE_ADMIN", "관리자"),
    USER("ROLE_USER", "사용자");

    private final String key;
    private final String value;

    public String getKey() {
        return key;
    }
}
