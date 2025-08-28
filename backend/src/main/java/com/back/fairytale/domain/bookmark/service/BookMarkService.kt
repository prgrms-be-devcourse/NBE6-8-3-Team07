package com.back.fairytale.domain.bookmark.service

import com.back.fairytale.domain.bookmark.dto.BookMarkDto
import com.back.fairytale.domain.bookmark.entity.BookMark
import com.back.fairytale.domain.bookmark.repository.BookMarkRepository
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.domain.bookmark.exception.BookMarkAlreadyExistsException
import com.back.fairytale.domain.bookmark.exception.BookMarkNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookMarkService(
    private val bookMarkRepository: BookMarkRepository,
    private val userRepository: UserRepository,
    private val fairytaleRepository: FairytaleRepository
) {

    @Transactional(readOnly = true)
    fun getBookMark(userId: Long): List<BookMarkDto> {
        return bookMarkRepository.findByUserId(userId).map { bookMark ->
            BookMarkDto(fairytaleId = bookMark.fairytale.id!!)
        }
    }

    @Transactional
    fun addBookMark(fairytaleId: Long, userId: Long): BookMark {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("해당 유저를 찾을 수 없습니다.")
        val fairytale = fairytaleRepository.findByIdOrNull(fairytaleId)
            ?: throw IllegalArgumentException("해당 동화를 찾을 수 없습니다.")

        bookMarkRepository.findByUserIdAndFairytaleId(user.id!!, fairytale.id!!)?.let {
            throw BookMarkAlreadyExistsException("이미 즐겨찾기에 추가된 동화입니다.")
        }

        val bookMark = BookMark.toEntity(user, fairytale)

        return bookMarkRepository.save(bookMark)
    }

    @Transactional
    fun removeBookMark(userId: Long, fairytaleId: Long) {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("해당 유저를 찾을수 없습니다.")
        val fairytale = fairytaleRepository.findByIdOrNull(fairytaleId)
            ?: throw IllegalArgumentException("해당 동화를 찾을 수 없습니다.")
        val bookMark = bookMarkRepository.findByUserIdAndFairytaleId(user.id!!, fairytale.id!!)
            ?: throw BookMarkNotFoundException("즐겨찾기에 없는 동화입니다.")

        bookMarkRepository.deleteById(bookMark.id!!)
    }

    @Transactional(readOnly = true)
    fun isBookmarked(userId: Long, fairytableId: Long): Boolean {
        return bookMarkRepository.findByUserIdAndFairytaleId(userId, fairytableId) != null
    }
}
