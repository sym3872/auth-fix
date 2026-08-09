package com.example.springblogapi.config;

import com.example.springblogapi.auth.User;
import com.example.springblogapi.auth.User.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security 설정, JWT 발급·검증, JWT 인증 필터를 한 파일에 모은다.
 * 세 클래스의 역할은 나뉘어 있지만 같은 보안 흐름에 속하므로 파일 수를 줄이기 위해 내부 클래스로 묶었다.
 */
@Configuration // Spring 설정과 Bean을 만드는 클래스라는 뜻이다.
@EnableWebSecurity // 모든 웹 요청에 Spring Security 필터를 적용한다.
public class SecurityConfig {

    /** 회원가입 시 BCrypt 암호화, 로그인 시 비밀번호 비교에 사용할 객체를 만든다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 같은 비밀번호도 매번 다른 해시가 생성되는 BCrypt 구현체를 반환한다.
        return new BCryptPasswordEncoder();
    }

    /** application.yml의 비밀키와 만료 시간을 받아 JWT 도구를 Spring Bean으로 만든다. */
    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiration-ms}") long expirationMilliseconds
    ) {
        // 이 Bean은 AuthController와 JWT 필터가 같은 서명 키를 사용하게 해 준다.
        return new JwtTokenProvider(base64Secret, expirationMilliseconds);
    }

    /** 공개 주소, 로그인 필수 주소, 세션 정책, JWT 필터 순서를 설정한다. */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository
    ) throws Exception {
        // 아래 내부 JWT 필터에 토큰 도구와 회원 저장소를 전달한다.
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, userRepository);

        // 쿠키 세션을 사용하지 않는 REST API이므로 CSRF를 끈다.
        http.csrf(AbstractHttpConfigurer::disable);

        // 서버는 로그인 세션을 저장하지 않고 요청마다 JWT를 다시 확인한다.
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // HTML 로그인 화면과 HTTP Basic 창을 사용하지 않고 JWT만 사용한다.
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        // 인증 없이 보호 API를 요청하면 로그인 화면 대신 HTTP 401을 보낸다.
        http.exceptionHandling(exception ->
                exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        );

        // 회원가입·로그인과 게시글 조회는 공개하고, 나머지 요청은 로그인을 요구한다.
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/auth/**", "/h2-console/**", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()
                .anyRequest().authenticated()
        );

        // H2 콘솔은 iframe을 사용하므로 같은 출처의 frame만 허용한다.
        http.headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
        );

        // URL 권한을 검사하기 전에 JWT로 로그인 사용자를 먼저 복원한다.
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // 지금까지 조립한 설정을 실제 필터 체인으로 완성한다.
        return http.build();
    }

    /**
     * JWT Access Token을 만들고 검증하는 공개 내부 클래스다.
     * AuthController에서도 사용해야 하므로 public으로 선언한다.
     */
    public static class JwtTokenProvider {

        /** JWT 위조 여부를 확인하는 HMAC 서명 키다. */
        private final SecretKey signingKey;

        /** Access Token이 유효한 시간이며 application.yml에서는 60분이다. */
        private final long expirationMilliseconds;

        /** Base64 비밀키와 유효 시간을 실제 JWT 생성에 사용할 값으로 준비한다. */
        public JwtTokenProvider(String base64Secret, long expirationMilliseconds) {
            // Base64 문자열을 원래 바이트 배열로 되돌린다.
            byte[] keyBytes = Decoders.BASE64.decode(base64Secret);

            // 바이트 배열로 HS256 계열 HMAC 서명 키를 만든다.
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);

            // 토큰 만료 시간 계산에 사용하도록 필드에 저장한다.
            this.expirationMilliseconds = expirationMilliseconds;
        }

        /** 로그인한 회원 이메일을 subject에 담은 JWT를 발급한다. */
        public String createToken(String email) {
            // 토큰을 만드는 현재 시각이다.
            Date now = new Date();

            // 현재 시각에 설정된 유효 시간을 더해 만료 시각을 만든다.
            Date expiration = new Date(now.getTime() + expirationMilliseconds);

            // 이메일, 발급 시각, 만료 시각, 서명을 넣어 최종 JWT 문자열을 반환한다.
            return Jwts.builder()
                    .subject(email)
                    .issuedAt(now)
                    .expiration(expiration)
                    .signWith(signingKey)
                    .compact();
        }

        /** JWT 서명과 만료 시간을 검증한 뒤 subject의 이메일을 반환한다. */
        public String getEmail(String token) {
            // parseSignedClaims 과정에서 위조·만료 토큰이면 JwtException이 발생한다.
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 로그인할 때 subject에 넣은 이메일을 꺼낸다.
            return claims.getSubject();
        }
    }

    /** 모든 HTTP 요청에서 Authorization 헤더의 JWT를 한 번 검사하는 내부 필터다. */
    private static class JwtAuthenticationFilter extends OncePerRequestFilter {

        /** JWT 검증과 이메일 추출을 담당한다. */
        private final JwtTokenProvider jwtTokenProvider;

        /** 토큰 이메일에 해당하는 실제 회원을 조회한다. */
        private final UserRepository userRepository;

        /** SecurityConfig가 필터에 필요한 두 객체를 전달한다. */
        private JwtAuthenticationFilter(
                JwtTokenProvider jwtTokenProvider,
                UserRepository userRepository
        ) {
            this.jwtTokenProvider = jwtTokenProvider;
            this.userRepository = userRepository;
        }

        /** 요청 한 건마다 Bearer Token을 확인하고 로그인 사용자를 SecurityContext에 저장한다. */
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            // Authorization 헤더 전체를 읽는다. 예: Bearer eyJ...
            String header = request.getHeader("Authorization");

            // 헤더가 없거나 Bearer 방식이 아니면 인증하지 않고 다음 필터로 이동한다.
            if (header == null || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // 앞의 "Bearer " 일곱 글자를 제거해 실제 JWT만 남긴다.
            String token = header.substring(7);
            String email;

            try {
                // 서명과 만료 시간을 통과한 토큰에서만 이메일을 얻는다.
                email = jwtTokenProvider.getEmail(token);
            } catch (JwtException | IllegalArgumentException exception) {
                // 위조·만료·형식 오류 토큰은 로그인 처리하지 않는다.
                filterChain.doFilter(request, response);
                return;
            }

            // 토큰 이메일로 H2에 실제 회원이 존재하는지 확인한다.
            User user = userRepository.findByEmail(email).orElse(null);

            // 회원이 있고 현재 요청이 아직 인증되지 않았을 때만 인증 정보를 만든다.
            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // principal에는 User, 비밀번호는 null, 별도 권한은 빈 목록을 넣는다.
                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(user, null, List.of());

                // 현재 요청 전용 빈 SecurityContext를 만든다.
                SecurityContext context = SecurityContextHolder.createEmptyContext();

                // SecurityContext에 로그인 회원의 인증 정보를 저장한다.
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }

            // 인증 성공 여부와 관계없이 남은 필터와 컨트롤러로 요청을 넘긴다.
            filterChain.doFilter(request, response);
        }
    }
}
