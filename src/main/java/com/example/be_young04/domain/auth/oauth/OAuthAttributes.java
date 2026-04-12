package com.example.be_young04.domain.auth.oauth;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuthAttributes {

    private String provider;
    private String providerId;
    private String loginId;
    private String name;
    private String email;
    private String profileImageUrl;
    private Map<String, Object> attributes;

    public static OAuthAttributes ofGithub(Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .provider("github")
                .providerId(String.valueOf(attributes.get("id")))
                .loginId((String) attributes.get("login"))
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .profileImageUrl((String) attributes.get("avatar_url"))
                .attributes(attributes)
                .build();
    }
}
