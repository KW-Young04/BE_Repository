package com.example.be_young04.domain.auth.oauth;

import com.example.be_young04.domain.user.entity.GithubUser;
import com.example.be_young04.domain.user.service.GithubUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final GithubUserService githubUserService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if (!"github".equals(registrationId)) {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth 제공자입니다.");
        }

        Long githubId = Long.valueOf(String.valueOf(oauth2User.getAttributes().get("id")));
        String username = (String) oauth2User.getAttributes().get("login");
        String profileImageUrl = (String) oauth2User.getAttributes().get("avatar_url");
        String accessToken = userRequest.getAccessToken().getTokenValue();

        GithubUser githubUser = githubUserService.saveOrUpdate(
                githubId,
                username,
                profileImageUrl,
                accessToken
        );

        return new CustomOAuth2User(
                githubUser.getGithubId(),
                githubUser.getUsername(),
                oauth2User.getAttributes()
        );
    }
}