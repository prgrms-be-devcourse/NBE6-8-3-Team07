package com.back.fairytale.domain.bookmark.exception;

// 즐겨 찾기가 이미 존재할 때 발생하는 예외 클래스
public class BookMarkAlreadyExistsException extends RuntimeException {
    public BookMarkAlreadyExistsException(String message) {
        super(message);
    }
}
