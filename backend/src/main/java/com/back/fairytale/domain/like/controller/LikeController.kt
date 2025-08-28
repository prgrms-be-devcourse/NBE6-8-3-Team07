package com.back.fairytale.domain.like.controller

import com.back.fairytale.domain.like.dto.LikeDto
import com.back.fairytale.domain.like.service.LikeService
import com.back.fairytale.global.security.CustomOAuth2User
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
class LikeController(
    private val likeService: LikeService
) {

    @GetMapping("/likes")
    fun getLikes(@AuthenticationPrincipal user: CustomOAuth2User): ResponseEntity<List<LikeDto>> {
        val likes = likeService.getLikes(user)
        return ResponseEntity.ok(likes)
    }

    @PostMapping("like/{fairytaleId}")
    fun addLike(@AuthenticationPrincipal user: CustomOAuth2User, @PathVariable fairytaleId: Long): ResponseEntity<String> {
        return try {
            likeService.addLike(user.id, fairytaleId)
            ResponseEntity.ok("게시물 $fairytaleId 좋아요가 추가되었습니다.")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @DeleteMapping("like/{fairytaleId}")
    fun removeLike(@AuthenticationPrincipal user: CustomOAuth2User, @PathVariable fairytaleId: Long): ResponseEntity<String> {
        return try {
            likeService.removeLike(user.id, fairytaleId)
            ResponseEntity.ok("게시물 $fairytaleId 좋아요가 해제되었습니다.")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}