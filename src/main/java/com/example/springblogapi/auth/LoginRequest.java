package com.example.springblogapi.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 로그인 API가 받는 이메일과 비밀번호 JSON이다. */
public record LoginRequest(
        @Schema(description = "회원가입한 이메일", example = "tester@example.com")
        @NotBlank @Email String email,
        @Schema(description = "회원가입 때 입력한 비밀번호", example = "pass1234")
        @NotBlank String password
) {
}
