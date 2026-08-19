package com.example.springblogapi.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** JpaRepository가 회원 저장과 조회의 기본 기능을 자동으로 제공하는 저장소다. */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 로그인할 회원을 이메일로 찾는다. */
    Optional<User> findByEmail(String email);

    /** 회원가입 전에 같은 이메일이 있는지 확인한다. */
    boolean existsByEmail(String email);
}
