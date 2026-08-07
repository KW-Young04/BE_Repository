package com.example.be_young04.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "GITHUB_USERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GithubUser {

    @Id
    @Column(name = "GITHUB_ID")
    private Long githubId;

    @Column(name = "USERNAME", nullable = false, unique = true)
    private String username;

    @Column(name = "PROFILE_IMAGE_URL")
    private String profileImageUrl;

    @Column(name = "ACCESS_TOKEN", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "REFRESH_TOKEN", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "TOKEN_EXPIRES_AT")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public void updateProfileAndToken(String username, String profileImageUrl, String accessToken) {
        this.username = username;
        this.profileImageUrl = profileImageUrl;
        this.accessToken = accessToken;
        this.updatedAt = LocalDateTime.now();
    }
}
