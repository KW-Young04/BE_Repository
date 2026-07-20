package com.example.be_young04.global.config;

import com.example.be_young04.domain.auth.oauth.CustomOAuth2UserService;
import com.example.be_young04.domain.auth.oauth.OAuth2LoginSuccessHandler;
import com.example.be_young04.global.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 활성화
                .cors(Customizer.withDefaults())

                // CSRF 비활성화 (JWT 사용 시 필수)
                .csrf(csrf -> csrf.disable())

                // 기본 로그인 방식 비활성화
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 1. OPTIONS 요청(Preflight)은 토큰 검사 없이 모두 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/",
                                "/error",
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/oauth2/**",
                                "/login/**",
                                "/h2-console/**",
                                // 2. 실시간 분석 API도 필요 시 permitAll에 추가 가능
                                "/api/analysis/realtime",
                                "/api/analysis/wcag"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // H2 콘솔 iframe 허용
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    // CORS 설정 Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 3. localhost:8080(Swagger UI) 및 5173(프론트엔드) 허용
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:8080",
                "http://127.0.0.1:8080"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        // 4. 클라이언트(Swagger/React)가 읽을 수 있는 응답 헤더 설정
        configuration.setExposedHeaders(List.of("Authorization", "Refresh-Token", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}