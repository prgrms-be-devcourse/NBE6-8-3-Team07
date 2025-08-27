package com.back.fairytale.domain.thoughts.service;

import com.back.fairytale.domain.fairytale.entity.Fairytale;
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository;
import com.back.fairytale.domain.thoughts.dto.ThoughtsRequest;
import com.back.fairytale.domain.thoughts.dto.ThoughtsResponse;
import com.back.fairytale.domain.thoughts.dto.ThoughtsUpdateRequest;
import com.back.fairytale.domain.thoughts.entity.Thoughts;
import com.back.fairytale.domain.thoughts.repository.ThoughtsRepository;
import com.back.fairytale.domain.user.entity.User;
import com.back.fairytale.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ThoughtsService {

    private final ThoughtsRepository thoughtsRepository;
    private final UserRepository userRepository;
    private final FairytaleRepository fairytaleRepository;

    // 공통 로직
    // 아이생각 조회 및 유저 확인
    private Thoughts findThoughtAndCheckUser(Long id, Long userId) {
        // 아이생각 조회
        Thoughts thoughts = thoughtsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Id가 " + id + "인 thoughts를 찾을 수 없습니다."));

        // 유저 확인
        if (!thoughts.getUser().getId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다. thoughts 작성자와 요청한 유저가 일치하지 않습니다.");
        }

        return thoughts;
    }

    // 아이생각 작성
    public ThoughtsResponse createThoughts(ThoughtsRequest request, Long userId) {
        // 유저와 동화조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Id가 " + userId + "인 유저를 찾을 수 없습니다."));
        Fairytale fairytale = fairytaleRepository.findById(request.fairytaleId())
                .orElseThrow(() -> new RuntimeException("Id가 " + request.fairytaleId() + "인 동화를 찾을 수 없습니다."));

        // 아이생각 생성
        Thoughts thoughts = Thoughts.of(fairytale, user, request);

        // 아이생각 저장
        Thoughts savedThoughts = thoughtsRepository.save(thoughts);

        // 응답 생성
        return ThoughtsResponse.from(savedThoughts);
    }

    // 아이생각 조회
    @Transactional(readOnly = true)
    public ThoughtsResponse getThoughts(Long id, Long userId) {
        // 아이생각 조회 및 유저 확인
        Thoughts thoughts = findThoughtAndCheckUser(id, userId);

        // 응답 생성
        return ThoughtsResponse.from(thoughts);
    }

    // 아이생각 수정
    public ThoughtsResponse updateThoughts(Long id, ThoughtsUpdateRequest request, Long userId) {
        // 아이생각 조회 및 유저 확인
        Thoughts thoughts = findThoughtAndCheckUser(id, userId);

        // 아이생각 수정
        thoughts.update(request.name(), request.content(), request.parentContent());

        // 응답 생성
        return ThoughtsResponse.from(thoughts);
    }

    // 아이생각 삭제
    public void deleteThoughts(Long id, Long userId) {
        // 아이생각 조회 및 유저 확인
        Thoughts thoughts = findThoughtAndCheckUser(id, userId);

        // 아이생각 삭제
        thoughtsRepository.delete(thoughts);

        log.info("thoughts가 성공적으로 삭제되었습니다. ID: {}", id);
    }

    public ThoughtsResponse getThoughtsByFairytaleId(Long fairytaleId, Long userId) {
        Thoughts thoughts = thoughtsRepository.findByFairytaleIdAndUserId(fairytaleId, userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 동화에 대한 아이생각을 찾을 수 없습니다."));

        return ThoughtsResponse.from(thoughts);
    }
}
