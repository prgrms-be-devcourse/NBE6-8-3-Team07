package com.back.fairytale.domain.like.service;

import com.back.fairytale.domain.fairytale.entity.Fairytale;
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository;
import com.back.fairytale.domain.like.dto.LikeDto;
import com.back.fairytale.domain.like.entity.Like;
import com.back.fairytale.domain.like.exception.LikeAlreadyExistsException;
import com.back.fairytale.domain.like.exception.LikeNotFoundException;
import com.back.fairytale.domain.like.repository.LikeRepository;
import com.back.fairytale.domain.user.entity.User;
import com.back.fairytale.domain.user.repository.UserRepository;
import com.back.fairytale.global.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final FairytaleRepository fairytaleRepository;

    // 나중에 스트림으로 리펙토링
    @Transactional
    public List<LikeDto> getLikes(CustomOAuth2User user) {
        List<Like> likes = likeRepository.findByUserId(user.getId());
        List<LikeDto> likeDtos = new ArrayList<>();

        for (Like like : likes) {
            likeDtos.add(LikeDto.builder()
                    .fairytaleId(like.getFairytale().getId())
                    .build());
        }

        return likeDtos;
    }

    @Transactional
    public Like addLike(Long userId, Long fairytaleId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

        Fairytale fairytale = fairytaleRepository.findByIdWithPessimisticLock(fairytaleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 동화를 찾을 수 없습니다."));

        Optional<Like> existLike = likeRepository.findByUserIdAndFairytaleId(user.getId(), fairytale.getId());
        if (existLike.isPresent()) {
            throw new LikeAlreadyExistsException("이미 좋아요를 누른 동화입니다.");
        }

        Like like = Like.toEntity(user, fairytale);

        // 좋아요 수 증가
        fairytale.increaseLikeCount();

        return likeRepository.save(like);
    }

    @Transactional
    public void removeLike(Long userId, Long fairytaleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        Fairytale fairytale = fairytaleRepository.findByIdWithPessimisticLock(fairytaleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 동화를 찾을 수 없습니다."));

        Like like = likeRepository.findByUserIdAndFairytaleId(user.getId(), fairytale.getId())
                .orElseThrow(() -> new LikeNotFoundException("좋아요가 없는 동화입니다."));

        // 좋아요 수 감소
        fairytale.decreaseLikeCount();

        likeRepository.deleteById(like.getId());

    }

    // 해당 사용자가 좋아요를 누른 동화인지 확인하는 메서드
    public boolean isLikedByUser(Long userId, Long fairytaleId) {
        return likeRepository.findByUserIdAndFairytaleId(userId, fairytaleId).isPresent();
    }
}
