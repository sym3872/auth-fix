package com.example.springblogapi.post;

import io.swagger.v3.oas.annotations.media.Schema;

/** API 응답에 필요한 게시글과 작성자 공개 정보만 담는 JSON 구조다. */
public record PostResponse(
        @Schema(description = "게시글 번호", example = "3") Long id,
        @Schema(description = "게시글 제목", example = "Swagger로 작성한 첫 게시글") String title,
        @Schema(description = "게시글 본문", example = "로그인 후 JWT 토큰을 등록하고 게시글을 작성했습니다.") String content,
        @Schema(description = "작성자 회원 번호", example = "1") Long authorId,
        @Schema(description = "작성자 닉네임", example = "테스터") String authorNickname
) {

    /** JPA 엔티티를 API에 반환하기 쉬운 응답 객체로 바꾼다. */
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                post.getAuthor().getNickname()
        );
    }
}
