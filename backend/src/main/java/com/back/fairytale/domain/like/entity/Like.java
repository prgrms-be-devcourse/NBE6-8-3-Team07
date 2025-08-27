package com.back.fairytale.domain.like.entity;

import com.back.fairytale.domain.fairytale.entity.Fairytale;
import com.back.fairytale.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@Table(name = "likes", indexes = {
        @Index(name = "idx_like_user_fairytale", columnList = "user_id, fairytale_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fairytale_id", nullable = false)
    private Fairytale fairytale;

    public static Like toEntity(User user, Fairytale fairytale) {
        return Like.builder()
                .user(user)
                .fairytale(fairytale)
                .build();
    }

}
