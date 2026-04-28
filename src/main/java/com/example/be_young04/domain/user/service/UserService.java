package com.example.be_young04.domain.user.service;

import com.example.be_young04.domain.user.entity.Role;
import com.example.be_young04.domain.user.entity.User;
import com.example.be_young04.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User saveOrUpdateGithubUser(
            String provider,
            String providerId,
            String loginId,
            String name,
            String email,
            String profileImageUrl
    ) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .map(user -> {
                    user.updateProfile(loginId, name, email, profileImageUrl);
                    return userRepository.save(user);
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .provider(provider)
                                .providerId(providerId)
                                .loginId(loginId)
                                .name(name)
                                .email(email)
                                .profileImageUrl(profileImageUrl)
                                .role(Role.USER)
                                .build()
                ));
    }

    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }
}
