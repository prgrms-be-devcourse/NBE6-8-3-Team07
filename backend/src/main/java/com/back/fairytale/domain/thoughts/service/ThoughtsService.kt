package com.back.fairytale.domain.thoughts.service

import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.thoughts.dto.ThoughtsRequest
import com.back.fairytale.domain.thoughts.dto.ThoughtsResponse
import com.back.fairytale.domain.thoughts.dto.ThoughtsUpdateRequest
import com.back.fairytale.domain.thoughts.entity.Thoughts
import com.back.fairytale.domain.thoughts.repository.ThoughtsRepository
import com.back.fairytale.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ThoughtsService (
    private val thoughtsRepository: ThoughtsRepository,
    private val userRepository: UserRepository,
    private val fairytaleRepository: FairytaleRepository,
) {
    // 공통 로직
    // 로깅 로직
    companion object {
        private val log = LoggerFactory.getLogger(ThoughtsService::class.java)
    }

    // 아이생각 조회 및 유저 확인
    private fun findThoughtAndCheckUser(id: Long, userId: Long): Thoughts {
        // 아이생각 조회
        val thoughts = thoughtsRepository.findById(id).orElse(null)
                ?: throw EntityNotFoundException("Id가 $id 인 thoughts를 찾을 수 없습니다.")

        // 유저 확인
        if (thoughts.user.id != userId) {
            throw IllegalStateException("접근 권한이 없습니다. thoughts 작성자와 요청한 유저가 일치하지 않습니다.")
        }

        return thoughts
    }

    // 아이생각 작성
    fun createThoughts(request: ThoughtsRequest, userId: Long): ThoughtsResponse {
        // 유저와 동화조회
        val user = userRepository.findById(userId).orElse(null)
            ?: throw EntityNotFoundException("Id가 $userId 인 유저를 찾을 수 없습니다.")
        val fairytale = fairytaleRepository.findById(request.fairytaleId).orElse(null)
            ?: throw EntityNotFoundException("Id가 ${request.fairytaleId} 인 동화를 찾을 수 없습니다.")

        // 아이생각 생성
        val thoughts = Thoughts(fairytale, user, request.name, request.content, request.parentContent)

        // 아이생각 저장
        val savedThoughts = thoughtsRepository.save(thoughts)

        // 응답 생성
        return ThoughtsResponse.from(savedThoughts)
    }

    // 아이생각 조회
    @Transactional(readOnly = true)
    fun getThoughts(id: Long, userId: Long): ThoughtsResponse {
        // 아이생각 조회 및 유저 확인
        val thoughts = findThoughtAndCheckUser(id, userId)

        // 응답 생성
        return ThoughtsResponse.from(thoughts)
    }

    // 아이생각 수정
    fun updateThoughts(id: Long, request: ThoughtsUpdateRequest, userId: Long): ThoughtsResponse {
        // 아이생각 조회 및 유저 확인
        val thoughts = findThoughtAndCheckUser(id, userId)

        // 아이생각 수정
        thoughts.update(request.name, request.content, request.parentContent)

        // 응답 생성
        return ThoughtsResponse.from(thoughts)
    }

    // 아이생각 삭제
    fun deleteThoughts(id: Long, userId: Long) {
        // 아이생각 조회 및 유저 확인
        val thoughts = findThoughtAndCheckUser(id, userId)

        // 아이생각 삭제
        thoughtsRepository.delete(thoughts)

        log.info("thoughts가 성공적으로 삭제되었습니다. ID: {}", id)
    }

    fun getThoughtsByFairytaleId(fairytaleId: Long, userId: Long): ThoughtsResponse {
        val thoughts = thoughtsRepository.findByFairytaleIdAndUserId(fairytaleId, userId)
            ?: throw EntityNotFoundException("해당 동화에 대한 아이생각을 찾을 수 없습니다.")

        return ThoughtsResponse.from(thoughts)
    }
}
