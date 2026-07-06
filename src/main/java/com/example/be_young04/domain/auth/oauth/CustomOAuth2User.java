package com.example.be_young04.domain.auth.oauth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private final Long githubId;
    private final String username;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(Long githubId, String username, Map<String, Object> attributes) {
        this.githubId = githubId;
        this.username = username;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return String.valueOf(githubId);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}