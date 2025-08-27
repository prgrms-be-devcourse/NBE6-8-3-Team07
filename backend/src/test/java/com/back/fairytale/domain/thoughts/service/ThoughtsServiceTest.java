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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThoughtsServiceTest {

    @Mock
    private ThoughtsRepository thoughtsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FairytaleRepository fairytaleRepository;

    @InjectMocks
    private ThoughtsService thoughtsService;

    private User testUser;
    private Fairytale testFairytale;
    private Thoughts testThought;
    private ThoughtsRequest testThoughtsRequest;
    private ThoughtsUpdateRequest testThoughtsUpdateRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .nickname("testUser")
                .build();

        testFairytale = Fairytale.builder()
                .id(100L)
                .build();

        testThoughtsRequest = new ThoughtsRequest(
                testFairytale.getId(), "New Thought Name", "New Thought Content", "New Parent Content");
        testThoughtsUpdateRequest = new ThoughtsUpdateRequest(
                "Updated Thought Name", "Updated Thought Content", "Updated Parent Content");

        // 실제 of 메소드를 사용한다고 가정하고, 그 결과로 생성될 객체를 미리 정의합니다.
        testThought = Thoughts.builder()
                .id(1L)
                .fairytale(testFairytale)
                .user(testUser)
                .name(testThoughtsRequest.name())
                .content(testThoughtsRequest.content())
                .parentContent(testThoughtsRequest.parentContent())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("아이생각 작성 성공")
    void createThoughts_success() {
        // Given
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(fairytaleRepository.findById(testFairytale.getId())).thenReturn(Optional.of(testFairytale));
        // Thoughts.of()는 static 메소드이므로 Mockito로 직접 모의하기 어렵습니다.
        // 여기서는 save 메소드가 호출될 때 어떤 Thoughts 객체가 저장될지 예상하고, 그 객체를 반환하도록 설정합니다.
        when(thoughtsRepository.save(any(Thoughts.class))).thenReturn(testThought);

        // When
        ThoughtsResponse response = thoughtsService.createThoughts(testThoughtsRequest, testUser.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testThought.getId());
        assertThat(response.name()).isEqualTo(testThought.getName());
        assertThat(response.content()).isEqualTo(testThought.getContent());
        // ThoughtsResponse에 nickname 필드가 있다면 추가
        // assertThat(response.nickname()).isEqualTo(testUser.getNickname());
        verify(thoughtsRepository, times(1)).save(any(Thoughts.class));
    }

    @Test
    @DisplayName("아이생각 작성 실패 - 유저를 찾을 수 없음")
    void createThoughts_userNotFound() {
        // Given
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> thoughtsService.createThoughts(testThoughtsRequest, testUser.getId()));
        assertThat(exception.getMessage()).contains("유저를 찾을 수 없습니다.");
        verify(thoughtsRepository, never()).save(any(Thoughts.class));
    }

    @Test
    @DisplayName("아이생각 작성 실패 - 동화를 찾을 수 없음")
    void createThoughts_fairytaleNotFound() {
        // Given
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(fairytaleRepository.findById(testFairytale.getId())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> thoughtsService.createThoughts(testThoughtsRequest, testUser.getId()));
        assertThat(exception.getMessage()).contains("동화를 찾을 수 없습니다.");
        verify(thoughtsRepository, never()).save(any(Thoughts.class));
    }

    @Test
    @DisplayName("아이생각 조회 성공")
    void getThoughts_success() {
        // Given
        when(thoughtsRepository.findById(testThought.getId())).thenReturn(Optional.of(testThought));

        // When
        ThoughtsResponse response = thoughtsService.getThoughts(testThought.getId(), testUser.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testThought.getId());
        assertThat(response.name()).isEqualTo(testThought.getName());
        assertThat(response.content()).isEqualTo(testThought.getContent());
    }

    @Test
    @DisplayName("아이생각 조회 실패 - 아이생각을 찾을 수 없음")
    void getThoughts_thoughtNotFound() {
        // Given
        when(thoughtsRepository.findById(testThought.getId())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> thoughtsService.getThoughts(testThought.getId(), testUser.getId()));
        assertThat(exception.getMessage()).contains("thoughts를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("아이생각 조회 실패 - 접근 권한 없음")
    void getThoughts_accessDenied() {
        // Given
        User anotherUser = User.builder().id(2L).nickname("anotherUser").build();
        when(thoughtsRepository.findById(testThought.getId())).thenReturn(Optional.of(testThought));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> thoughtsService.getThoughts(testThought.getId(), anotherUser.getId()));
        assertThat(exception.getMessage()).contains("접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("아이생각 수정 성공")
    void updateThoughts_success() {
        // Given
        when(thoughtsRepository.findById(testThought.getId())).thenReturn(Optional.of(testThought));

        // When
        ThoughtsResponse response = thoughtsService.updateThoughts(testThought.getId(), testThoughtsUpdateRequest, testUser.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(testThoughtsUpdateRequest.name());
        assertThat(response.content()).isEqualTo(testThoughtsUpdateRequest.content());
        assertThat(response.parentContent()).isEqualTo(testThoughtsUpdateRequest.parentContent());
        assertThat(testThought.getName()).isEqualTo(testThoughtsUpdateRequest.name()); // 엔티티의 내용도 업데이트되었는지 확인
    }

    @Test
    @DisplayName("아이생각 수정 실패 - 아이생각을 찾을 수 없음")
    void updateThoughts_thoughtNotFound() {
        // Given
        when(thoughtsRepository.findById(testThought.getId())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> thoughtsService.updateThoughts(testThought.getId(), testThoughtsUpdateRequest, testUser.getId()));
        assertThat(exception.getMessage()).contains("thoughts를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("아이생각 수정 실패 - 접근 권한 없음")
    void updateThoughts_accessDenied() {
        // Given
        User anotherUser = User.builder().id(2L).nickname("anotherUser").build();
        when(thoughtsRepository.findById(testThought.getId())).thenReturn(Optional.of(testThought));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> thoughtsService.updateThoughts(testThought.getId(), testThoughtsUpdateRequest, anotherUser.getId()));
        assertThat(exception.getMessage()).contains("접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("아이생각 삭제 성공")
    void deleteThoughts_success() {
        // Given
        when(thoughtsRepository.findById(testThought.getId())).thenReturn(Optional.of(testThought));
        doNothing().when(thoughtsRepository).delete(any(Thoughts.class));

        // When
        thoughtsService.deleteThoughts(testThought.getId(), testUser.getId());

        // Then
        verify(thoughtsRepository, times(1)).delete(testThought);
    }

    @Test
    @DisplayName("아이생각 삭제 실패 - 아이생각을 찾을 수 없음")
    void deleteThoughts_thoughtNotFound() {
        // Given
        when(thoughtsRepository.findById(testThought.getId())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> thoughtsService.deleteThoughts(testThought.getId(), testUser.getId()));
        assertThat(exception.getMessage()).contains("thoughts를 찾을 수 없습니다.");
        verify(thoughtsRepository, never()).delete(any(Thoughts.class));
    }

    @Test
    @DisplayName("아이생각 삭제 실패 - 접근 권한 없음")
    void deleteThoughts_accessDenied() {
        // Given
        User anotherUser = User.builder().id(2L).nickname("anotherUser").build();
        when(thoughtsRepository.findById(testThought.getId())).thenReturn(Optional.of(testThought));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> thoughtsService.deleteThoughts(testThought.getId(), anotherUser.getId()));
        assertThat(exception.getMessage()).contains("접근 권한이 없습니다.");
        verify(thoughtsRepository, never()).delete(any(Thoughts.class));
    }
}
