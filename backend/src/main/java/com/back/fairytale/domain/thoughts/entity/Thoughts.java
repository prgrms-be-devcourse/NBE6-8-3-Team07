package com.back.fairytale.domain.thoughts.entity;

import com.back.fairytale.domain.fairytale.entity.Fairytale;
import com.back.fairytale.domain.thoughts.dto.ThoughtsRequest;
import com.back.fairytale.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "thoughts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Thoughts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fairytale_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Fairytale fairytale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // 아이 이름
    @Column(nullable = false, length = 50)
    private String name;

    // 아이 생각
    @Lob
    @Column(nullable = false)
    private String content;

    // 부모 생각
    @Lob
    @Column(nullable = false)
    private String parentContent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Thoughts 생성
    public static Thoughts of(Fairytale fairytale, User user, ThoughtsRequest request) {
        return Thoughts.builder()
                .fairytale(fairytale)
                .user(user)
                .name(request.name())
                .content(request.content())
                .parentContent(request.parentContent())
                .build();
    }

    // Thoughts 수정
    public void update(String name, String content, String parentContent) {
        // 아이이름 수정
        if (name != null) {
            this.name = name.trim();
        }
        // 아이생각 수정
        if (content != null) {
            this.content = content.trim();
        }
        // 부모생각 수정
        if (parentContent != null) {
            this.parentContent = parentContent.trim();
        }
    }
}
