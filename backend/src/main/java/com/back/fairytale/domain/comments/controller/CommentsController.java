package com.back.fairytale.domain.comments.controller;

import com.back.fairytale.domain.comments.dto.CommentsRequest;
import com.back.fairytale.domain.comments.dto.CommentsResponse;
import com.back.fairytale.domain.comments.dto.CommentsUpdateRequest;
import com.back.fairytale.domain.comments.service.CommentsService;
import com.back.fairytale.global.security.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "댓글 API", description = "동화에 대한 댓글을 작성, 조회, 수정, 삭제하는 API입니다.")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentsController {

    private final CommentsService commentsService;

    // 댓글 작성
    @Operation(summary = "댓글 작성", description = "동화에 대한 댓글을 작성합니다.")
    @PostMapping("/fairytales/{fairytaleId}/comments")
    public ResponseEntity<CommentsResponse> createComments(
            @PathVariable Long fairytaleId,
            @Valid@RequestBody CommentsRequest request,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        CommentsResponse response = commentsService.createComments(request, customOAuth2User.getId());

        // 새로 생성된 리소스의 URI를 생성
        URI location = URI.create("/api/fairytales/" + fairytaleId + "/comments/" + response.id());

        // 201 Created 상태 코드와 Location 헤더, 응답 본문을 함께 반환
        return ResponseEntity.created(location).body(response);
    }

    // 댓글 조회
    @Operation(summary = "댓글 조회", description = "동화에 대한 댓글을 조회합니다.")
    @GetMapping("/fairytales/{fairytaleId}/comments")
    public ResponseEntity<Page<CommentsResponse>> getComments(
            @PathVariable Long fairytaleId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CommentsResponse> response = commentsService.getCommentsByFairytale(fairytaleId, pageable);

        return ResponseEntity.ok(response);
    }

    // 댓글 수정
    @Operation(summary = "댓글 수정", description = "동화에 대한 댓글을 수정합니다.")
    @PatchMapping("/comments/{id}")
    public ResponseEntity<CommentsResponse> updateComments(
            @PathVariable Long id,
            @Valid @RequestBody CommentsUpdateRequest request,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        CommentsResponse response = commentsService.updateComments(id, request, customOAuth2User.getId());

        return ResponseEntity.ok(response);
    }

    // 댓글 삭제
    @Operation(summary = "댓글 삭제", description = "동화에 대한 댓글을 삭제합니다.")
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComments(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        commentsService.deleteComments(id, customOAuth2User.getId());

        return ResponseEntity.noContent().build();
    }
}
