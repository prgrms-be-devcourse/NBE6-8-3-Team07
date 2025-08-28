package com.back.fairytale.domain.bookmark.controller

import com.back.fairytale.domain.bookmark.dto.BookMarkDto
import com.back.fairytale.domain.bookmark.service.BookMarkService
import com.back.fairytale.global.security.CustomOAuth2User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
class BookMarkController(
    private val bookMarkService: BookMarkService
) {

    @GetMapping("/bookmarks")
    fun getFavorites(@AuthenticationPrincipal oAuth2User: CustomOAuth2User): ResponseEntity<List<BookMarkDto>> {
        val favorites = bookMarkService.getBookMark(oAuth2User.id)
        return ResponseEntity.ok(favorites)
    }

    @PostMapping("/bookmark/{fairytaleId}")
    fun addFavorite(
        @AuthenticationPrincipal oAuth2User: CustomOAuth2User,
        @PathVariable fairytaleId: Long
    ): ResponseEntity<String> {
        return try {
            bookMarkService.addBookMark(fairytaleId, oAuth2User.id)
            ResponseEntity.status(HttpStatus.CREATED).body("게시물 " + fairytaleId + " 즐겨찾기에 추가되었습니다.")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @DeleteMapping("/bookmark/{fairytaleId}")
    fun removeFavorite(
        @AuthenticationPrincipal oAuth2User: CustomOAuth2User,
        @PathVariable fairytaleId: Long
    ): ResponseEntity<String> {
        return try {
            bookMarkService.removeBookMark(oAuth2User.id, fairytaleId)
            ResponseEntity.ok("게시물 " + fairytaleId + " 즐겨찾기에서 해제되었습니다.")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}
