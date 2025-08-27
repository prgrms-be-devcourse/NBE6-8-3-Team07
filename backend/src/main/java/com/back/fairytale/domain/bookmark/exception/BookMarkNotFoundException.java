package com.back.fairytale.domain.bookmark.exception;

// 즐겨 찾기가 존재하지 않을 때 발생하는 예외 클래스
public class BookMarkNotFoundException extends RuntimeException {
    public BookMarkNotFoundException(String message) {
        super(message);
    }
}
