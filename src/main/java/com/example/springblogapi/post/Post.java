package com.example.springblogapi.post;

import com.example.springblogapi.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 블로그 게시글 한 건을 H2 데이터베이스의 posts 테이블에 저장하기 위한 엔티티다.
 * 엔티티는 자바 객체와 데이터베이스 테이블의 한 행을 연결해 주므로 SQL을 직접 작성하지 않아도 된다.
 */
// @Entity는 Post 객체 한 개를 posts 테이블의 한 행과 연결한다.
@Entity
// 테이블 이름을 명확하게 posts로 지정한다.
@Table(name = "posts")
public class Post {

    /** 데이터베이스가 게시글마다 자동으로 부여하는 고유 번호다. */
    @Id
    // 게시글을 저장할 때 H2가 id 값을 자동으로 증가시킨다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 목록에서 보이는 게시글 제목이며, 빈 제목이 저장되지 않도록 데이터베이스에서도 필수 값으로 지정한다. */
    @Column(nullable = false, length = 100)
    private String title;

    /** 게시글 본문이며, 긴 글도 저장할 수 있게 데이터베이스의 TEXT 타입을 사용한다. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 이 게시글을 작성한 회원이다.
     * 여러 게시글은 한 명의 회원에게 속할 수 있으므로 다대일(@ManyToOne) 관계를 사용한다.
     * LAZY는 게시글을 읽을 때 작성자 정보까지 항상 바로 가져오지 않아 불필요한 조회를 줄이는 설정이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    // posts 테이블의 author_id 열이 users 테이블의 회원 id를 가리킨다.
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * JPA가 데이터베이스에서 객체를 만들 때 사용할 기본 생성자다.
     * 직접 새 게시글을 만들 때는 아래의 세 개 인자를 받는 생성자를 사용한다.
     */
    protected Post() {
    }

    /**
     * 새 게시글을 만들 때 제목, 본문, 작성자를 한 번에 넣는다.
     * 작성자를 함께 저장해야 나중에 본인 글인지 비교해 수정과 삭제 권한을 판단할 수 있다.
     */
    public Post(String title, String content, User author) {
        // 생성자 인자를 새 게시글 객체의 필드에 각각 저장한다.
        this.title = title;
        this.content = content;
        this.author = author;
    }

    /**
     * 수정 요청이 왔을 때 제목과 본문만 바꾼다.
     * 작성자는 글의 소유자이므로 수정 과정에서 바꾸지 않는다.
     */
    public void update(String title, String content) {
        // JPA 트랜잭션 안에서 필드를 바꾸면 변경된 값이 UPDATE SQL에 반영된다.
        this.title = title;
        this.content = content;
    }

    /** 게시글 고유 번호를 외부 코드에서 읽을 수 있게 한다. */
    public Long getId() {
        return id;
    }

    /** 게시글 제목을 외부 코드에서 읽을 수 있게 한다. */
    public String getTitle() {
        return title;
    }

    /** 게시글 본문을 외부 코드에서 읽을 수 있게 한다. */
    public String getContent() {
        return content;
    }

    /** 게시글 작성자를 외부 코드에서 읽을 수 있게 한다. */
    public User getAuthor() {
        return author;
    }

    /**
     * Post 전용 CRUD 저장소를 같은 파일에 묶어 파일 수를 줄였다.
     * JpaRepository가 save, findAll, findById, delete를 자동으로 제공한다.
     */
    public interface PostRepository extends JpaRepository<Post, Long> {
        // 현재 프로젝트는 JpaRepository의 기본 CRUD만 사용하므로 추가 메서드가 없다.
    }
}
