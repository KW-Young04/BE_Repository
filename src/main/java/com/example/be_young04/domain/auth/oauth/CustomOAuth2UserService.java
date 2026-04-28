package com.example.be_young04.domain.auth.oauth;

import com.example.be_young04.domain.user.entity.User;
import com.example.be_young04.domain.user.service.UserService;
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

    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if (!"github".equals(registrationId)) {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth 제공자입니다.");
        }

        OAuthAttributes attributes = OAuthAttributes.ofGithub(oauth2User.getAttributes());

        User user = userService.saveOrUpdateGithubUser(
                attributes.getProvider(),
                attributes.getProviderId(),
                attributes.getLoginId(),
                attributes.getName(),
                attributes.getEmail(),
                attributes.getProfileImageUrl()
        );

        return new CustomOAuth2User(
                user.getId(),
                user.getLoginId(),
                user.getRole(),
                attributes.getAttributes()
        );
    }
}