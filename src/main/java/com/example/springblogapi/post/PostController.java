package com.example.springblogapi.post;

import com.example.springblogapi.auth.User;
import com.example.springblogapi.post.Post.PostRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * /api/posts 주소로 들어오는 게시글 관련 HTTP 요청을 처리하는 컨트롤러다.
 * 초보자가 흐름을 따라가기 쉽도록 서비스 클래스를 따로 만들지 않고, 이 예제에서는 Repository를 바로 사용한다.
 */
// @RestController는 반환한 PostResponse를 Spring이 JSON으로 바꾸게 한다.
@RestController
// 이 클래스의 모든 API 주소 앞에는 /api/posts가 붙는다.
@RequestMapping("/api/posts")
public class PostController {

    /** 게시글 데이터를 저장하고 찾는 역할을 Spring이 주입해 준다. */
    private final PostRepository postRepository;

    /**
     * 컨트롤러가 생성될 때 PostRepository를 받아 둔다.
     * 생성자 주입은 필요한 객체가 없으면 애플리케이션이 시작할 때 바로 알려 주기 때문에 실수를 찾기 쉽다.
     */
    public PostController(PostRepository postRepository) {
        // Spring이 만든 Repository 객체를 필드에 저장한다.
        this.postRepository = postRepository;
    }

    /**
     * 로그인한 사용자가 새 게시글을 작성하는 API다.
     * JWT Security 단계가 합쳐지면 getCurrentLoginUser 메서드가 토큰에서 현재 회원을 찾아 작성자로 넣는다.
     */
    // POST /api/posts 요청을 이 메서드와 연결한다.
    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        // JWT 필터가 SecurityContext에 넣은 현재 로그인 회원을 가져온다.
        User currentUser = getCurrentLoginUser();

        // 요청 JSON의 제목·내용과 로그인 회원을 묶어 새 Post 객체를 만든다.
        Post post = new Post(request.title(), request.content(), currentUser);

        // save를 호출하면 JPA가 posts 테이블에 INSERT하고 id가 채워진 객체를 반환한다.
        Post savedPost = postRepository.save(post);

        // 저장된 글을 응답용 record로 바꾸고 생성 성공 상태 201과 함께 보낸다.
        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.from(savedPost));
    }

    /**
     * 누구나 게시글 목록을 볼 수 있는 API다.
     * SecurityConfig에서 이 주소의 GET 요청을 permitAll로 설정하면 로그인하지 않아도 접근할 수 있다.
     * 작성자 정보를 지연 로딩해도 응답 객체로 바꾸는 동안 연결이 유지되도록 읽기 전용 트랜잭션을 사용한다.
     */
    // GET /api/posts 요청을 목록 조회 메서드와 연결한다.
    @GetMapping
    // readOnly는 이 메서드가 데이터를 수정하지 않는 조회 작업임을 표시한다.
    @Transactional(readOnly = true)
    public List<PostResponse> getPosts() {
        // findAll로 모든 Post를 읽고 각 Post를 안전한 응답 객체로 변환해 List로 만든다.
        return postRepository.findAll().stream()
                // PostResponse::from은 각 post에 from(post)를 적용한다는 짧은 문법이다.
                .map(PostResponse::from)
                .toList();
    }

    /**
     * 게시글 번호로 한 건의 상세 내용을 조회하는 API다.
     * 없는 번호를 요청하면 최소한의 예외로 잘못된 요청임을 알린다.
     * 작성자 정보를 응답으로 바꾸는 동안 연결을 유지하도록 읽기 전용 트랜잭션을 사용한다.
     */
    // {id} 부분에는 사용자가 요청한 게시글 번호가 들어간다.
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public PostResponse getPost(@PathVariable Long id) {
        // @PathVariable이 URL의 글 번호를 Long id 매개변수에 넣어 준다.
        Post post = findPostById(id);

        // 엔티티 자체 대신 공개할 필드만 들어 있는 응답 객체를 반환한다.
        return PostResponse.from(post);
    }

    /**
     * 로그인한 사용자가 자기 게시글의 제목과 본문을 수정하는 API다.
     * 작성자가 다르면 저장하지 않아 다른 사람의 글을 바꾸지 못하게 한다.
     * 조회한 게시글과 작성자 정보를 같은 트랜잭션 안에서 비교하고 저장하기 위해 트랜잭션을 사용한다.
     */
    // PUT /api/posts/{id} 요청을 게시글 수정 메서드와 연결한다.
    @PutMapping("/{id}")
    // 조회, 권한 검사, 수정을 하나의 작업 단위로 묶는다.
    @Transactional
    public PostResponse updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        // 1단계: JWT로 인증된 현재 사용자를 가져온다.
        User currentUser = getCurrentLoginUser();

        // 2단계: 수정할 게시글이 실제로 존재하는지 조회한다.
        Post post = findPostById(id);

        // 3단계: 현재 사용자와 글 작성자가 같은지 확인한다.
        checkWriter(post, currentUser);

        // 4단계: 검사를 모두 통과한 경우에만 제목과 본문을 변경한다.
        post.update(request.title(), request.content());

        // 변경된 엔티티를 저장하고 최신 객체를 받는다.
        Post savedPost = postRepository.save(post);

        // 수정된 결과를 JSON 응답용 형태로 바꿔 반환한다.
        return PostResponse.from(savedPost);
    }

    /**
     * 로그인한 사용자가 자기 게시글을 삭제하는 API다.
     * 작성자가 아닌 경우에는 삭제하지 않아 본인만 삭제할 수 있게 한다.
     * 조회·작성자 비교·삭제를 하나의 트랜잭션으로 처리한다.
     */
    // DELETE /api/posts/{id} 요청을 삭제 메서드와 연결한다.
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        // 1단계: 현재 로그인 회원을 확인한다.
        User currentUser = getCurrentLoginUser();

        // 2단계: 삭제할 게시글을 조회한다.
        Post post = findPostById(id);

        // 3단계: 작성자 본인인지 확인하고, 아니면 403 오류를 발생시킨다.
        checkWriter(post, currentUser);

        // 4단계: 검사를 통과한 게시글을 데이터베이스에서 삭제한다.
        postRepository.delete(post);

        // 삭제 응답에는 본문이 필요 없으므로 HTTP 204 No Content를 반환한다.
        return ResponseEntity.noContent().build();
    }

    /**
     * 여러 API에서 반복되는 게시글 조회 코드를 한 곳에 모은 작은 보조 메서드다.
     * 존재하지 않는 게시글이면 이후 코드가 null을 사용하다 실패하지 않도록 즉시 예외를 낸다.
     */
    private Post findPostById(Long id) {
        // Optional에 값이 있으면 Post를 반환하고, 없으면 아래 404 예외를 만든다.
        return postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "게시글을 찾을 수 없습니다. id=" + id
                ));
    }

    /**
     * 현재 로그인한 회원과 게시글 작성자가 같은지 검사한다.
     * id를 비교하는 이유는 데이터베이스에서 같은 회원을 서로 다른 자바 객체로 읽어도 정확히 본인임을 판단하기 위해서다.
     */
    private void checkWriter(Post post, User currentUser) {
        // 게시글의 author.id와 JWT로 찾은 currentUser.id가 다른지 비교한다.
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            // 다른 회원의 글이면 403 Forbidden으로 수정과 삭제를 막는다.
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "작성자 본인만 수정하거나 삭제할 수 있습니다."
            );
        }
    }

    /**
     * JWT 검증 필터가 SecurityContext에 넣어 둔 현재 로그인 사용자를 가져온다.
     * 이 메서드는 POST, PUT, DELETE에서만 호출되며, SecurityConfig가 해당 요청에 로그인을 요구한다.
     */
    private User getCurrentLoginUser() {
        // 현재 요청의 인증 결과를 SecurityContext에서 읽는다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // JWT 필터가 넣은 principal이 User인지 확인해 안전하게 형 변환한다.
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            // 보안 설정상 이 상황은 발생하지 않아야 하지만, 혹시 모를 잘못된 호출은 즉시 막는다.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 게시글 작성자와 비교할 실제 로그인 회원을 반환한다.
        return user;
    }

    /**
     * 게시글 작성 JSON의 제목과 본문을 받는 작은 자료 상자다.
     * record가 생성자와 값을 읽는 메서드를 자동으로 만들어 주므로 별도 DTO 파일을 만들지 않는다.
     */
    public record CreatePostRequest(
            // 제목과 내용은 공백만 입력하는 것도 허용하지 않는다.
            @NotBlank(message = "제목은 비어 있을 수 없습니다.") String title,
            @NotBlank(message = "내용은 비어 있을 수 없습니다.") String content
    ) {
    }

    /** 게시글 수정 JSON의 새 제목과 본문을 받는 자료 상자다. */
    public record UpdatePostRequest(
            // 수정 요청도 제목과 내용이 모두 있어야 한다.
            @NotBlank(message = "제목은 비어 있을 수 없습니다.") String title,
            @NotBlank(message = "내용은 비어 있을 수 없습니다.") String content
    ) {
    }

    /** 게시글과 공개 가능한 작성자 정보만 JSON으로 반환하는 자료 상자다. */
    public record PostResponse(
            Long id,
            String title,
            String content,
            Long authorId,
            String authorNickname
    ) {
        /** Post 엔티티에서 응답에 필요한 값만 골라 복사한다. */
        public static PostResponse from(Post post) {
            // 비밀번호 같은 User의 민감한 정보는 넣지 않고 작성자 id와 닉네임만 넣는다.
            return new PostResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getAuthor().getId(),
                    post.getAuthor().getNickname()
            );
        }
    }
}
