package com.example.springblogapi.post;

import org.springframework.data.jpa.repository.JpaRepository;

/** JpaRepository가 게시글 저장, 조회, 삭제의 기본 기능을 자동으로 제공하는 저장소다. */
public interface PostRepository extends JpaRepository<Post, Long> {
}
