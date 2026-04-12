package com.example.be_young04.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String providerId;   // GitHub의 고유 사용자 식별값

    @Column(nullable = false)
    private String provider;     // github

    @Column(nullable = false)
    private String loginId;      // github login

    private String name;

    private String email;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public void updateProfile(String loginId, String name, String email, String profileImageUrl) {
        this.loginId = loginId;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }
}
