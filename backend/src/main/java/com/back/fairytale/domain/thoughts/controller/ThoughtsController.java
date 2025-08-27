package com.back.fairytale.domain.thoughts.controller;

import com.back.fairytale.domain.thoughts.dto.ThoughtsRequest;
import com.back.fairytale.domain.thoughts.dto.ThoughtsResponse;
import com.back.fairytale.domain.thoughts.dto.ThoughtsUpdateRequest;
import com.back.fairytale.domain.thoughts.service.ThoughtsService;
import com.back.fairytale.global.security.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "아이생각 API", description = "아이생각을 작성, 조회, 수정, 삭제하는 API입니다.")
@RestController
@RequestMapping("/api/thoughts")
@RequiredArgsConstructor
public class ThoughtsController {

    private final ThoughtsService thoughtsService;

    // 아이생각 작성
    @Operation(summary = "아이생각 작성", description = "동화에 대한 아이생각을 작성합니다.")
    @PostMapping
    public ResponseEntity<ThoughtsResponse> createThoughts(
            @Valid @RequestBody ThoughtsRequest request,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        ThoughtsResponse response = thoughtsService.createThoughts(request, customOAuth2User.getId());

        // 새로 생성된 리소스의 URI를 생성
        URI location = URI.create("/api/thoughts/" + response.id());

        // 201 Created 상태 코드와 Location 헤더, 응답 본문을 함께 반환
        return ResponseEntity.created(location).body(response);
}

    // 아이생각 조회
    @Operation(summary = "아이생각 조회", description = "아이생각을 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ThoughtsResponse> getThoughts(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        ThoughtsResponse response = thoughtsService.getThoughts(id, customOAuth2User.getId());

        return ResponseEntity.ok(response);
    }

    // 동화별 아이생각 조회
    @Operation(summary = "동화별 아이생각 조회", description = "특정 동화에 대한 아이생각을 조회합니다.")
    @GetMapping("/fairytale/{fairytaleId}")
    public ResponseEntity<ThoughtsResponse> getThoughtsByFairytaleId(
            @PathVariable Long fairytaleId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        ThoughtsResponse response = thoughtsService.getThoughtsByFairytaleId(fairytaleId, customOAuth2User.getId());
        return ResponseEntity.ok(response);
    }

    // 아이생각 수정
    @Operation(summary = "아이생각 수정", description = "아이생각을 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ThoughtsResponse> updateThoughts(
            @PathVariable Long id,
            @Valid @RequestBody ThoughtsUpdateRequest request,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        ThoughtsResponse response = thoughtsService.updateThoughts(id, request, customOAuth2User.getId());
        return ResponseEntity.ok(response);
    }

    // 아이생각 삭제
    @Operation(summary = "아이생각 삭제", description = "아이생각을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteThoughts(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        thoughtsService.deleteThoughts(id, customOAuth2User.getId());
        return ResponseEntity.noContent().build();
    }
}
