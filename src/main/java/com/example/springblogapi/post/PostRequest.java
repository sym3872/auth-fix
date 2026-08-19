package com.example.springblogapi.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 게시글 작성과 수정 API가 공통으로 받는 제목과 본문 JSON이다. */
public record PostRequest(
        @Schema(description = "게시글 제목", example = "Swagger로 작성한 첫 게시글")
        @NotBlank @Size(max = 100) String title,
        @Schema(description = "게시글 본문", example = "로그인 후 JWT 토큰을 등록하고 게시글을 작성했습니다.")
        @NotBlank String content
) {
}
