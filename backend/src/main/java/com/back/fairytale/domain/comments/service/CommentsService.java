package com.back.fairytale.domain.comments.service;

import com.back.fairytale.domain.comments.dto.CommentsRequest;
import com.back.fairytale.domain.comments.dto.CommentsResponse;
import com.back.fairytale.domain.comments.dto.CommentsUpdateRequest;
import com.back.fairytale.domain.comments.entity.Comments;
import com.back.fairytale.domain.comments.repository.CommentsRepository;
import com.back.fairytale.domain.fairytale.entity.Fairytale;
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository;
import com.back.fairytale.domain.user.entity.User;
import com.back.fairytale.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CommentsService {

    private final CommentsRepository commentsRepository;
    private final UserRepository userRepository;
    private final FairytaleRepository fairytaleRepository;

    // 공통 로직
    // 댓글 조회 및 유저 확인
    private Comments findCommentAndCheckUser(Long id, Long userId) {
        // 댓글 조회
        Comments comments = commentsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Id가 " + id + "인 댓글을 찾을 수 없습니다."));

        // 유저 확인
        if (!comments.getUser().getId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다. 댓글 작성자와 요청한 유저가 일치하지 않습니다.");
        }

        return comments;
    }

    // 댓글 작성
    public CommentsResponse createComments(CommentsRequest request, Long userId) {
        // 유저와 동화 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Id가 " + userId + "인 유저를 찾을 수 없습니다."));
        Fairytale fairytale = fairytaleRepository.findById(request.fairytaleId())
                .orElseThrow(() -> new RuntimeException("Id가 " + request.fairytaleId() + "인 동화를 찾을 수 없습니다."));

        // 댓글 생성
        Comments comments = Comments.of(fairytale, user, request.content());

        // 댓글 저장
        Comments savedComments = commentsRepository.save(comments);

        // 응답 생성
        return CommentsResponse.from(savedComments);
    }

    // 댓글 조회
    @Transactional(readOnly = true)
    public Page<CommentsResponse> getCommentsByFairytale(Long fairytaleId, Pageable pageable) {
        // 동화 조회
        Fairytale fairytale = fairytaleRepository.findById(fairytaleId)
                .orElseThrow(() -> new RuntimeException("Id가 " + fairytaleId + "인 동화를 찾을 수 없습니다."));

        // 댓글 조회
        Page<Comments> commentsPage = commentsRepository.findByFairytale(fairytale, pageable);

        // 응답 생성
        return commentsPage.map(CommentsResponse::from);
    }

    // 댓글 수정
    public CommentsResponse updateComments(Long id, CommentsUpdateRequest request, Long userId) {
        // 댓글 조회 및 유저 확인
        Comments comments = findCommentAndCheckUser(id, userId);

        // 댓글 수정
        comments.updateContent(request.content());

        // 응답 생성
        return CommentsResponse.from(comments);
    }

    // 댓글 삭제
    public void deleteComments(Long id, Long userId) {
        // 댓글 조회 및 유저 확인
        Comments comments = findCommentAndCheckUser(id, userId);

        // 댓글 삭제
        commentsRepository.delete(comments);

        log.info("댓글이 성공적으로 삭제되었습니다. ID: {}", id);
    }
}
