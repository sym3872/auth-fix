package com.example.springblogapi.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** H2의 users 테이블 한 행을 표현하는 회원 엔티티다. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // 평문이 아니라 BCrypt로 암호화된 문자열만 저장한다.
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    /** JPA가 데이터베이스 값으로 객체를 만들 때 필요한 기본 생성자다. */
    protected User() {
    }

    /** 회원가입 때 받은 값을 새 회원 객체에 저장한다. */
    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }
}
