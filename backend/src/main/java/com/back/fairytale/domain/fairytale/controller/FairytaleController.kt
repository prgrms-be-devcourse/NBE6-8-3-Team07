package com.back.fairytale.domain.fairytale.controller

import com.back.fairytale.domain.fairytale.dto.FairytaleCreateRequest
import com.back.fairytale.domain.fairytale.dto.FairytalePublicListResponse
import com.back.fairytale.domain.fairytale.exception.FairytaleNotFoundException
import com.back.fairytale.domain.fairytale.exception.UserNotFoundException
import com.back.fairytale.domain.fairytale.service.FairytaleService
import com.back.fairytale.global.security.CustomOAuth2User
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/fairytales")
class FairytaleController(
    private val fairytaleService: FairytaleService
) {

    // 동화 생성
    @PostMapping
    fun createFairytale(
        @Valid @RequestBody request: FairytaleCreateRequest,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User
    ): ResponseEntity<Any> {
        return try {
            val userId = customOAuth2User.id
            val response = fairytaleService.createFairytale(request, userId)
            ResponseEntity.ok(response)
        } catch (e: UserNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        }
    }

    // 동화 전체 조회
    @GetMapping
    fun getAllFairytales(
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User
    ): ResponseEntity<Any> {
        return try {
            val userId = customOAuth2User.id
            val response = fairytaleService.getAllFairytalesByUserId(userId)
            ResponseEntity.ok(response)
        } catch (e: FairytaleNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        }
    }

    // 동화 상세 조회
    @GetMapping("/{id}")
    fun getFairytaleById(
        @PathVariable id: Long,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User
    ): ResponseEntity<Any> {
        return try {
            val userId = customOAuth2User.id
            val response = fairytaleService.getFairytaleByIdAndUserId(id, userId)
            ResponseEntity.ok(response)
        } catch (e: FairytaleNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        }
    }

    // 동화 삭제
    @DeleteMapping("/{id}")
    fun deleteFairytale(
        @PathVariable id: Long,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User
    ): ResponseEntity<Any> {
        return try {
            val userId = customOAuth2User.id
            fairytaleService.deleteFairytaleByIdAndUserId(id, userId)
            ResponseEntity.noContent().build()
        } catch (e: FairytaleNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        }
    }

    // 갤러리에서 공개 동화 조회
    @GetMapping("/gallery")
    fun getPublicFairytalesForGallery(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "6") size: Int
    ): ResponseEntity<Any> {
        return try {
            val pageable: Pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
            val response: Page<FairytalePublicListResponse> = fairytaleService.getPublicFairytalesForGallery(pageable)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("갤러리 조회 중 오류가 발생했습니다.")
        }
    }

    // 특정 사용자의 공개 동화 조회
    @GetMapping("/gallery/user/{userId}")
    fun getPublicFairytalesByUserId(
        @PathVariable userId: Long
    ): ResponseEntity<Any> {
        return try {
            val response = fairytaleService.getPublicFairytalesByUserId(userId)
            ResponseEntity.ok(response)
        } catch (e: FairytaleNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        }
    }

    // 공개 동화 상세 조회 (갤러리용)
    @GetMapping("/gallery/{id}")
    fun getPublicFairytaleById(
        @PathVariable id: Long
    ): ResponseEntity<Any> {
        return try {
            val response = fairytaleService.getPublicFairytaleById(id)
            ResponseEntity.ok(response)
        } catch (e: FairytaleNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        }
    }

    // 동화 공개/비공개 설정
    @PatchMapping("/{id}/visibility")
    fun updateFairytaleVisibility(
        @PathVariable id: Long,
        @RequestParam isPublic: Boolean,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User
    ): ResponseEntity<Any> {
        return try {
            val userId = customOAuth2User.id
            fairytaleService.updateFairytaleVisibility(id, userId, isPublic)
            ResponseEntity.ok().build()
        } catch (e: FairytaleNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        }
    }
}