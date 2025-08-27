package com.back.fairytale.global.security.port;

/**
 * 로그아웃 이후 유저 도메인에 대해 추가적인 작업이 필요할 시 구현하는 인터페이스입니다.
 */
public interface LogoutService {
    void logout(Long userId);
}
