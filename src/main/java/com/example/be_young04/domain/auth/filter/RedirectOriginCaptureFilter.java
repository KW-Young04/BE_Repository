package com.example.be_young04.domain.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RedirectOriginCaptureFilter extends OncePerRequestFilter {

    public static final String SESSION_KEY = "REDIRECT_ORIGIN";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/oauth2/authorization/")) {
            String redirectOrigin = request.getParameter("redirectOrigin");
            if (redirectOrigin != null && !redirectOrigin.isBlank()) {
                request.getSession().setAttribute(SESSION_KEY, redirectOrigin);
            }
        }

        filterChain.doFilter(request, response);
    }
}