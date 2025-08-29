package com.back.fairytale.domain.like.exception

// 좋아요가 존재하지 않을 때 발생하는 예외 클래스
class LikeNotFoundException(message: String) : RuntimeException(message)