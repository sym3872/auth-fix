package com.example.springblogapi.auth;

/** 회원가입 뒤 비밀번호를 제외하고 반환하는 회원 정보다. */
public record SignupResponse(Long id, String email, String nickname) {
}
