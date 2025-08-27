package com.back.fairytale.domain.like.exception;

// 좋아요 가 이미 존재할 때 발생하는 예외 클래스
public class LikeAlreadyExistsException extends RuntimeException {
    public LikeAlreadyExistsException(String message) {
        super(message);
    }
}
