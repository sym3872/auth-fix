package com.example.springblogapi.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;

/** JWT를 만들고 서명·만료 시간을 검증하는 작은 도구다. */
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMilliseconds;

    /** application.yml에서 받은 Base64 비밀키와 만료 시간으로 JWT 도구를 준비한다. */
    public JwtTokenProvider(String base64Secret, long expirationMilliseconds) {
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMilliseconds = expirationMilliseconds;
    }

    /** 이메일을 subject로 넣고 현재 시각과 만료 시각에 서명한 Access Token을 만든다. */
    public String createToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMilliseconds))
                .signWith(signingKey)
                .compact();
    }

    /** 위조되었거나 만료된 토큰은 예외를 발생시키고, 정상 토큰의 이메일을 반환한다. */
    public String getEmail(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
