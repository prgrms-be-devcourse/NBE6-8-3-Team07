package com.back.fairytale.domain.like.exception;

// 좋아요가 존재하지 않을 때 발생하는 예외 클래스
public class LikeNotFoundException extends RuntimeException {
    public LikeNotFoundException(String message) {
        super(message);
    }
}
