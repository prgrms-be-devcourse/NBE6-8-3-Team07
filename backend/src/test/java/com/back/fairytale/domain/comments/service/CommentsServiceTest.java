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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentsServiceTest {

    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FairytaleRepository fairytaleRepository;

    @InjectMocks
    private CommentsService commentsService;

    private User testUser;
    private Fairytale testFairytale;
    private Comments testComment;
    private CommentsRequest testCommentsRequest;
    private CommentsUpdateRequest testCommentsUpdateRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .nickname("testUser")
                .build();

        testFairytale = Fairytale.builder()
                .id(100L)
                .build();

        testComment = Comments.builder()
                .id(1L)
                .fairytale(testFairytale)
                .user(testUser)
                .content("Original comment content")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testCommentsRequest = new CommentsRequest(testFairytale.getId(), "New comment content");
        testCommentsUpdateRequest = new CommentsUpdateRequest("Updated comment content");
    }

    @Test
    @DisplayName("댓글 작성 성공")
    void createComments_success() {
        // Given
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(fairytaleRepository.findById(testFairytale.getId())).thenReturn(Optional.of(testFairytale));
        when(commentsRepository.save(any(Comments.class))).thenReturn(testComment);

        // When
        CommentsResponse response = commentsService.createComments(testCommentsRequest, testUser.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testComment.getId());
        assertThat(response.content()).isEqualTo(testComment.getContent());
        assertThat(response.nickname()).isEqualTo(testUser.getNickname());
        verify(commentsRepository, times(1)).save(any(Comments.class));
    }

    @Test
    @DisplayName("댓글 작성 실패 - 유저를 찾을 수 없음")
    void createComments_userNotFound() {
        // Given
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentsService.createComments(testCommentsRequest, testUser.getId()));
        assertThat(exception.getMessage()).contains("유저를 찾을 수 없습니다.");
        verify(commentsRepository, never()).save(any(Comments.class));
    }

    @Test
    @DisplayName("댓글 작성 실패 - 동화를 찾을 수 없음")
    void createComments_fairytaleNotFound() {
        // Given
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(fairytaleRepository.findById(testFairytale.getId())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentsService.createComments(testCommentsRequest, testUser.getId()));
        assertThat(exception.getMessage()).contains("동화를 찾을 수 없습니다.");
        verify(commentsRepository, never()).save(any(Comments.class));
    }

    @Test
    @DisplayName("동화별 댓글 조회 성공")
    void getCommentsByFairytale_success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Comments> commentsList = Arrays.asList(testComment,
                Comments.builder().id(2L).fairytale(testFairytale).user(testUser).content("Another comment").build());
        Page<Comments> commentsPage = new PageImpl<>(commentsList, pageable, commentsList.size());

        when(fairytaleRepository.findById(testFairytale.getId())).thenReturn(Optional.of(testFairytale));
        when(commentsRepository.findByFairytale(testFairytale, pageable)).thenReturn(commentsPage);

        // When
        Page<CommentsResponse> responsePage = commentsService.getCommentsByFairytale(testFairytale.getId(), pageable);

        // Then
        assertThat(responsePage).isNotNull();
        assertThat(responsePage.getTotalElements()).isEqualTo(commentsList.size());
        assertThat(responsePage.getContent()).hasSize(commentsList.size());
        assertThat(responsePage.getContent().get(0).id()).isEqualTo(testComment.getId());
        assertThat(responsePage.getContent().get(0).nickname()).isEqualTo(testUser.getNickname());
    }

    @Test
    @DisplayName("동화별 댓글 조회 실패 - 동화를 찾을 수 없음")
    void getCommentsByFairytale_fairytaleNotFound() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(fairytaleRepository.findById(testFairytale.getId())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentsService.getCommentsByFairytale(testFairytale.getId(), pageable));
        assertThat(exception.getMessage()).contains("동화를 찾을 수 없습니다.");
        verify(commentsRepository, never()).findByFairytale(any(Fairytale.class), any(Pageable.class));
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComments_success() {
        // Given
        when(commentsRepository.findById(testComment.getId())).thenReturn(Optional.of(testComment));
        // testComment의 user 필드가 실제 User 객체를 참조하도록 setUp에서 설정했습니다.

        // When
        CommentsResponse response = commentsService.updateComments(testComment.getId(), testCommentsUpdateRequest, testUser.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo(testCommentsUpdateRequest.content());
        assertThat(testComment.getContent()).isEqualTo(testCommentsUpdateRequest.content()); // 엔티티의 내용도 업데이트되었는지 확인
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글을 찾을 수 없음")
    void updateComments_commentNotFound() {
        // Given
        when(commentsRepository.findById(testComment.getId())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentsService.updateComments(testComment.getId(), testCommentsUpdateRequest, testUser.getId()));
        assertThat(exception.getMessage()).contains("댓글을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("댓글 수정 실패 - 접근 권한 없음")
    void updateComments_accessDenied() {
        // Given
        User anotherUser = User.builder().id(2L).nickname("anotherUser").build();
        when(commentsRepository.findById(testComment.getId())).thenReturn(Optional.of(testComment));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentsService.updateComments(testComment.getId(), testCommentsUpdateRequest, anotherUser.getId()));
        assertThat(exception.getMessage()).contains("접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComments_success() {
        // Given
        when(commentsRepository.findById(testComment.getId())).thenReturn(Optional.of(testComment));
        doNothing().when(commentsRepository).delete(any(Comments.class));

        // When
        commentsService.deleteComments(testComment.getId(), testUser.getId());

        // Then
        verify(commentsRepository, times(1)).delete(testComment);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글을 찾을 수 없음")
    void deleteComments_commentNotFound() {
        // Given
        when(commentsRepository.findById(testComment.getId())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentsService.deleteComments(testComment.getId(), testUser.getId()));
        assertThat(exception.getMessage()).contains("댓글을 찾을 수 없습니다.");
        verify(commentsRepository, never()).delete(any(Comments.class));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 접근 권한 없음")
    void deleteComments_accessDenied() {
        // Given
        User anotherUser = User.builder().id(2L).nickname("anotherUser").build();
        when(commentsRepository.findById(testComment.getId())).thenReturn(Optional.of(testComment));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentsService.deleteComments(testComment.getId(), anotherUser.getId()));
        assertThat(exception.getMessage()).contains("접근 권한이 없습니다.");
        verify(commentsRepository, never()).delete(any(Comments.class));
    }
}
