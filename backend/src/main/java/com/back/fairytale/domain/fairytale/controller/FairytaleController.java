package com.back.fairytale.domain.fairytale.controller;

import com.back.fairytale.domain.fairytale.dto.*;
import com.back.fairytale.domain.fairytale.exception.FairytaleNotFoundException;
import com.back.fairytale.domain.fairytale.exception.UserNotFoundException;
import com.back.fairytale.domain.fairytale.service.FairytaleService;
import com.back.fairytale.global.security.CustomOAuth2User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fairytales")
@RequiredArgsConstructor
public class FairytaleController {

    private final FairytaleService fairytaleService;

    // 동화 생성
    @PostMapping
    public ResponseEntity<?> createFairytale(
            @Valid @RequestBody FairytaleCreateRequest request,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        try {
            Long userId = customOAuth2User.getId();

            FairytaleResponse response = fairytaleService.createFairytale(request, userId);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 동화 전체 조회
    @GetMapping
    public ResponseEntity<?> getAllFairytales(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        try {
            Long userId = customOAuth2User.getId();

            List<FairytaleListResponse> response = fairytaleService.getAllFairytalesByUserId(userId);
            return ResponseEntity.ok(response);
        } catch (FairytaleNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 동화 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getFairytaleById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        try {
            Long userId = customOAuth2User.getId();

            FairytaleDetailResponse response = fairytaleService.getFairytaleByIdAndUserId(id, userId);
            return ResponseEntity.ok(response);
        } catch (FairytaleNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 동화 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFairytale(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        try {
            Long userId = customOAuth2User.getId();

            fairytaleService.deleteFairytaleByIdAndUserId(id, userId);
            return ResponseEntity.noContent().build();
        } catch (FairytaleNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 갤러리에서 공개 동화 조회
    @GetMapping("/gallery")
    public ResponseEntity<?> getPublicFairytalesForGallery(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<FairytalePublicListResponse> response = fairytaleService.getPublicFairytalesForGallery(pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("갤러리 조회 중 오류가 발생했습니다.");
        }
    }


    // 특정 사용자의 공개 동화 조회
    @GetMapping("/gallery/user/{userId}")
    public ResponseEntity<?> getPublicFairytalesByUserId(@PathVariable Long userId) {
        try {
            List<FairytalePublicListResponse> response = fairytaleService.getPublicFairytalesByUserId(userId);
            return ResponseEntity.ok(response);
        } catch (FairytaleNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 공개 동화 상세 조회 (갤러리용)
    @GetMapping("/gallery/{id}")
    public ResponseEntity<?> getPublicFairytaleById(@PathVariable Long id) {
        try {
            FairytaleDetailResponse response = fairytaleService.getPublicFairytaleById(id);
            return ResponseEntity.ok(response);
        } catch (FairytaleNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 동화 공개/비공개 설정
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<?> updateFairytaleVisibility(
            @PathVariable Long id,
            @RequestParam Boolean isPublic,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        try {
            Long userId = customOAuth2User.getId();

            fairytaleService.updateFairytaleVisibility(id, userId, isPublic);
            return ResponseEntity.ok().build();

        } catch (FairytaleNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}