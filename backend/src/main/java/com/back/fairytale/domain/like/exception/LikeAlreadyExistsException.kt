package com.back.fairytale.domain.like.exception

// 좋아요 가 이미 존재할 때 발생하는 예외 클래스
class LikeAlreadyExistsException(message: String) : RuntimeException(message)