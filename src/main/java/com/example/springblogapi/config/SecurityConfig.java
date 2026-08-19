package com.example.springblogapi.config;

import com.example.springblogapi.auth.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security의 URL 권한, 비밀번호 암호화, JWT 필터 순서를 설정한다.
 * JWT 구현과 Swagger 문서 설정은 각각 JwtTokenProvider와 SwaggerConfig가 담당한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Swagger의 보호 API가 참조할 JWT Bearer 인증 방식 이름이다. */
    public static final String BEARER_AUTH = "bearerAuth";

    /** 회원가입 비밀번호를 BCrypt로 암호화하고 로그인 때 비교할 도구다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** application.yml의 비밀키와 만료 시간으로 JWT 발급·검증 도구를 만든다. */
    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiration-ms}") long expirationMilliseconds
    ) {
        return new JwtTokenProvider(base64Secret, expirationMilliseconds);
    }

    /**
     * 공개 주소, 로그인 필수 주소, 세션 정책, JWT 필터 순서를 설정한다.
     * POST·PUT·DELETE 게시글 API는 마지막 규칙에 남겨 반드시 인증을 거치게 한다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository
    ) throws Exception {
        // @Component 없이 만든 필터를 Security 필터 체인에 한 번만 넣는다.
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider, userRepository);

        // JWT를 쓰는 REST API는 서버 세션과 CSRF 폼 인증을 사용하지 않는다.
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        // 로그인이 필요한 주소에 토큰이 없으면 로그인 페이지 대신 401을 돌려준다.
        http.exceptionHandling(exception ->
                exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        );

        http.authorizeHttpRequests(authorize -> authorize
                // 회원가입·로그인·Swagger·H2 콘솔은 토큰 없이 열어 둔다.
                .requestMatchers(
                        "/api/auth/**",
                        "/h2-console/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-helper.js",
                        "/v3/api-docs/**",
                        "/error"
                ).permitAll()
                // 게시글 읽기만 공개하고 작성·수정·삭제는 아래 authenticated 규칙으로 보낸다.
                .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()
                .anyRequest().authenticated()
        );

        // H2 콘솔은 iframe을 사용하므로 같은 사이트 안에서만 frame을 허용한다.
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        // URL 권한 검사 전에 Authorization 헤더의 JWT로 로그인 User를 복원한다.
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
