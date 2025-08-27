package com.back.fairytale.domain.bookmark.controller;

import com.back.fairytale.domain.bookmark.dto.BookMarkDto;
import com.back.fairytale.domain.bookmark.service.BookMarkService;
import com.back.fairytale.global.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookMarkController {

    private final BookMarkService bookMarkService;

    @GetMapping("/bookmarks")
    public ResponseEntity<List<BookMarkDto>> getFavorites(@AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        List<BookMarkDto> favorites = bookMarkService.getBookMark(oAuth2User.getId());
        return ResponseEntity.ok(favorites);
    }

    @PostMapping("/bookmark/{fairytaleId}")
    public ResponseEntity<String> addFavorite(@AuthenticationPrincipal CustomOAuth2User oAuth2User, @PathVariable Long fairytaleId) {
        try {
            bookMarkService.addBookMark(fairytaleId, oAuth2User.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body("게시물 " + fairytaleId + " 즐겨찾기에 추가되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/bookmark/{fairytaleId}")
    public ResponseEntity<String> removeFavorite(@AuthenticationPrincipal CustomOAuth2User oAuth2User, @PathVariable Long fairytaleId) {
        try {
            bookMarkService.removeBookMark(oAuth2User.getId(), fairytaleId);
            return ResponseEntity.ok("게시물 " + fairytaleId + " 즐겨찾기에서 해제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}