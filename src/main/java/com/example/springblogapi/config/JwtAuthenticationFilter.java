package com.example.springblogapi.config;

import com.example.springblogapi.auth.User;
import com.example.springblogapi.auth.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 매 요청의 Authorization: Bearer 토큰을 검사하는 필터다.
 * @Component를 붙이지 않고 SecurityConfig에서 한 번만 필터 체인에 등록한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /** 토큰 도구와 회원 저장소를 받아, 토큰 이메일로 실제 회원을 찾을 준비를 한다. */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    /** Bearer 토큰이 정상일 때만 SecurityContext에 로그인 User를 저장한다. */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        // 공개 요청 또는 Bearer 형식이 아닌 요청은 인증을 만들지 않고 다음 단계로 넘긴다.
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtTokenProvider.getEmail(header.substring(7));
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(user, null, List.of())
                );
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // 잘못된 토큰은 로그인으로 인정하지 않는다. 보호 주소에서는 이후 401이 반환된다.
        }

        filterChain.doFilter(request, response);
    }
}
