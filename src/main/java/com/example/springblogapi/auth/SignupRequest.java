package com.example.springblogapi.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 회원가입 API가 받는 이메일, 비밀번호, 닉네임 JSON이다. */
public record SignupRequest(
        @Schema(description = "로그인에 사용할 이메일", example = "tester@example.com")
        @NotBlank @Email String email,
        @Schema(description = "4글자 이상 비밀번호", example = "pass1234")
        @NotBlank @Size(min = 4) String password,
        @Schema(description = "화면에 표시할 닉네임", example = "테스터")
        @NotBlank String nickname
) {
}
