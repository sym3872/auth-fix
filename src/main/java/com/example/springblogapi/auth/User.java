package com.example.springblogapi.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 한 명의 정보를 H2 데이터베이스의 users 테이블에 저장하기 위한 엔티티다.
 *
 * 비밀번호는 평문을 저장하면 안 되므로, AuthController에서 BCrypt로 암호화한 값을
 * password 필드에 넣는다. 이 클래스는 사용자 정보 자체를 표현하는 역할만 맡는다.
 */
// @Entity는 이 클래스가 데이터베이스 테이블과 연결되는 JPA 객체임을 뜻한다.
@Entity
// 실제 테이블 이름을 users로 지정한다.
@Table(name = "users") // USER는 일부 데이터베이스에서 예약어일 수 있어 안전한 테이블 이름을 사용한다.
public class User {

    /** 데이터베이스가 자동으로 만들어 주는 회원의 고유 번호다. */
    @Id
    // IDENTITY 전략은 INSERT할 때 데이터베이스가 1, 2, 3처럼 번호를 증가시킨다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인할 때 사용하는 이메일이며, 같은 이메일로 두 번 가입하지 못하게 한다. */
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt로 암호화된 비밀번호를 저장한다. 평문 비밀번호를 저장하면 안 된다. */
    @Column(nullable = false)
    private String password;

    /** 화면에 보여 줄 회원의 별명이다. */
    @Column(nullable = false)
    private String nickname;

    /**
     * JPA가 데이터베이스에서 User 객체를 다시 만들 때 사용하는 기본 생성자다.
     * JPA 규칙 때문에 protected 또는 public 기본 생성자가 필요하다.
     */
    protected User() {
    }

    /**
     * 회원가입할 때 받은 정보를 새 User 객체로 묶는다.
     * password에는 이미 암호화된 비밀번호가 전달되어야 한다.
     */
    public User(String email, String password, String nickname) {
        // this.email은 객체의 필드, 오른쪽 email은 생성자로 받은 값이다.
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    /** 데이터베이스의 회원 고유 번호를 읽을 때 사용한다. */
    public Long getId() {
        // 필드를 private으로 숨기고 필요한 값만 getter로 읽게 한다.
        return id;
    }

    /** 로그인 아이디인 이메일을 읽을 때 사용한다. */
    public String getEmail() {
        return email;
    }

    /** 암호화된 비밀번호를 로그인 검증에 사용할 때 읽는다. */
    public String getPassword() {
        return password;
    }

    /** 회원의 별명을 읽을 때 사용한다. */
    public String getNickname() {
        return nickname;
    }

    /**
     * User 전용 데이터베이스 기능을 같은 파일에 묶어 파일 수를 줄인 저장소다.
     * 내부 인터페이스지만 public이므로 AuthController와 SecurityConfig에서도 사용할 수 있다.
     */
    public interface UserRepository extends JpaRepository<User, Long> {

        /** 이메일로 회원을 찾으며, 회원이 없을 수도 있어 Optional로 반환한다. */
        Optional<User> findByEmail(String email);

        /** 회원가입 전에 같은 이메일이 이미 저장되어 있는지 확인한다. */
        boolean existsByEmail(String email);
    }
}
