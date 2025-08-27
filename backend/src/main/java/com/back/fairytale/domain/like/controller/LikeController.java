package com.back.fairytale.domain.like.controller;

import com.back.fairytale.domain.like.dto.LikeDto;
import com.back.fairytale.domain.like.service.LikeService;
import com.back.fairytale.global.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @GetMapping("/likes")
    public ResponseEntity<List<LikeDto>> getLikes(@AuthenticationPrincipal CustomOAuth2User user) {
        List<LikeDto> likes = likeService.getLikes(user);
        return ResponseEntity.ok(likes);
    }

    @PostMapping("like/{fairytaleId}")
    public ResponseEntity<String> addLike(@AuthenticationPrincipal CustomOAuth2User user, @PathVariable Long fairytaleId) {
        try {
            likeService.addLike(user.getId(), fairytaleId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok("게시물 " + fairytaleId + " 좋아요가 추가되었습니다.");
    }

    @DeleteMapping("like/{fairytaleId}")
    public ResponseEntity<String> removeLike(@AuthenticationPrincipal CustomOAuth2User user, @PathVariable Long fairytaleId) {
        try {
            likeService.removeLike(user.getId(), fairytaleId);
            return ResponseEntity.ok("게시물 " + fairytaleId + " 좋아요가 해제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}