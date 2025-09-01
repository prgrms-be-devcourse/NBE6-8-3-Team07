package com.back.fairytale.domain.comments.controller

import com.back.fairytale.domain.comments.dto.CommentsRequest
import com.back.fairytale.domain.comments.dto.CommentsResponse
import com.back.fairytale.domain.comments.dto.CommentsUpdateRequest
import com.back.fairytale.domain.comments.service.CommentsService
import com.back.fairytale.global.security.CustomOAuth2User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URI

@Tag(name = "댓글 API", description = "동화에 대한 댓글 및 대댓글을 작성, 조회, 수정, 삭제하는 API입니다.")
@RestController
@RequestMapping("/api")
class CommentsController (
    private val commentsService: CommentsService
) {
    // 댓글 작성
    @Operation(summary = "댓글 및 대댓글 작성", description = "동화에 대한 댓글을 작성합니다. parentId를 포함하면 대댓글이 작성됩니다.")
    @PostMapping("/fairytales/{fairytaleId}/comments")
    fun createComments(
        @PathVariable fairytaleId: Long,
        @RequestBody request: @Valid CommentsRequest,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User
    ): ResponseEntity<CommentsResponse> {
        // URL과 요청 본문의 fairytaleId가 일치하는지 검증
        if (fairytaleId != request.fairytaleId) {
            throw IllegalArgumentException("URL과 요청 본문의 동화 Id가 일치하지 않습니다.")
        }

        val response = commentsService.createComments(request, customOAuth2User.id)

        // 새로 생성된 리소스의 URI를 생성
        val location = URI.create("/api/fairytales/" + fairytaleId + "/comments/" + response.id)

        // 201 Created 상태 코드와 Location 헤더, 응답 본문을 함께 반환
        return ResponseEntity.created(location).body(response)
    }

    // 댓글 조회
    @Operation(summary = "댓글 조회", description = "동화의 댓글을 계층 구조로 조회합니다. 부모-자식 관계가 유지된 상태로 반환됩니다.")
    @GetMapping("/fairytales/{fairytaleId}/comments")
    fun getComments(
        @PathVariable fairytaleId: Long,
        @PageableDefault(size = 10) pageable: Pageable
    ): ResponseEntity<Page<CommentsResponse>> =
        ResponseEntity.ok(commentsService.getCommentsByFairytale(fairytaleId, pageable))

    // 댓글 수정
    @Operation(summary = "댓글 수정", description = "동화에 대한 댓글을 수정합니다.")
    @PatchMapping("/comments/{id}")
    fun updateComments(
        @PathVariable id: Long,
        @RequestBody request: @Valid CommentsUpdateRequest,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User
    ): ResponseEntity<CommentsResponse> =
        ResponseEntity.ok(commentsService.updateComments(id, request, customOAuth2User.id))

    // 댓글 삭제
    @Operation(summary = "댓글 삭제", description = "동화에 대한 댓글을 삭제합니다.")
    @DeleteMapping("/comments/{id}")
    fun deleteComments(
        @PathVariable id: Long,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User
    ): ResponseEntity<Void> {
        commentsService.deleteComments(id, customOAuth2User.id)

        return ResponseEntity.noContent().build<Void>()
    }
}
