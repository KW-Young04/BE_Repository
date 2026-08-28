package com.example.be_young04.domain.auth.oauth;

import com.example.be_young04.global.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.example.be_young04.domain.auth.filter.RedirectOriginCaptureFilter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.oauth2.redirect-uri}")
    private String defaultRedirectUri;

    private static final List<String> ALLOWED_ORIGIN_PATTERNS = List.of(
            "http://localhost:5173",
            "https://kw-codee.vercel.app"
    );

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        CustomOAuth2User user = (CustomOAuth2User) authentication.getPrincipal();

        String serviceAccessToken = jwtTokenProvider.createAccessToken(
                user.getGithubId(),
                user.getUsername()
        );

        String origin = (String) request.getSession()
                .getAttribute(RedirectOriginCaptureFilter.SESSION_KEY);

        String targetUri = resolveTargetUri(origin);

        response.sendRedirect(
                targetUri + "?token=" + URLEncoder.encode(serviceAccessToken, StandardCharsets.UTF_8)
        );
    }

    private String resolveTargetUri(String origin) {
        if (origin != null && ALLOWED_ORIGIN_PATTERNS.contains(origin)) {
            return origin + "/repository-connect";
        }
        return defaultRedirectUri;
    }
}