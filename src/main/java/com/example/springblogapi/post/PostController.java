package com.example.springblogapi.post;

import com.example.springblogapi.auth.User;
import com.example.springblogapi.config.SecurityConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

/** 게시글 CRUD API의 요청 흐름과 작성자 권한 확인을 담당하는 컨트롤러다. */
@RestController
@RequestMapping("/api/posts")
@Tag(name = "Post", description = "게시글 CRUD API")
public class PostController {

    private final PostRepository postRepository;

    /** Spring이 게시글 저장소를 생성자로 넣어 준다. */
    public PostController(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /** 로그인 사용자를 작성자로 넣어 새 게시글을 저장한다. */
    @PostMapping
    @Operation(
            summary = "게시글 작성",
            description = "로그인 후 받은 JWT 문자열만 오른쪽 위 토큰 입력 버튼에 한 번 붙여넣고 작성하세요."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "게시글 작성 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PostResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "제목 또는 본문 입력이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "JWT 토큰이 없거나 올바르지 않음")
    })
    @SecurityRequirement(name = SecurityConfig.BEARER_AUTH)
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest request) {
        Post savedPost = postRepository.save(
                new Post(request.title(), request.content(), currentUser())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.from(savedPost));
    }

    /** 누구나 게시글 목록을 볼 수 있다. LAZY 작성자 정보를 읽기 위해 트랜잭션을 연다. */
    @GetMapping
    @Operation(summary = "게시글 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공")
    })
    @Transactional(readOnly = true)
    public List<PostResponse> getPosts() {
        return postRepository.findAll().stream().map(PostResponse::from).toList();
    }

    /** 게시글 한 건을 공개 조회한다. */
    @GetMapping("/{id}")
    @Operation(summary = "게시글 상세 조회")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PostResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @Transactional(readOnly = true)
    public PostResponse getPost(@PathVariable Long id) {
        return PostResponse.from(findPost(id));
    }

    /** 로그인한 작성자 본인만 제목과 본문을 수정할 수 있다. */
    @PutMapping("/{id}")
    @Operation(
            summary = "게시글 수정",
            description = "① 직접 실행을 누릅니다. ② 수정할 글 번호를 입력합니다. ③ '현재 글 불러오기'를 누르면 "
                    + "아래 제목과 본문이 채워집니다. ④ 필요한 부분만 바꾼 뒤 실행하세요."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 수정 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PostResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "제목 또는 본문 입력이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "JWT 토큰이 없거나 올바르지 않음"),
            @ApiResponse(responseCode = "403", description = "작성자 본인만 수정하거나 삭제할 수 있음"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @SecurityRequirement(name = SecurityConfig.BEARER_AUTH)
    @Transactional
    public PostResponse updatePost(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        Post post = findPost(id);
        checkWriter(post);
        post.update(request.title(), request.content());
        return PostResponse.from(post);
    }

    /** 로그인한 작성자 본인만 게시글을 삭제할 수 있다. */
    @DeleteMapping("/{id}")
    @Operation(summary = "게시글 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "게시글 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "JWT 토큰이 없거나 올바르지 않음"),
            @ApiResponse(responseCode = "403", description = "작성자 본인만 수정하거나 삭제할 수 있음"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @SecurityRequirement(name = SecurityConfig.BEARER_AUTH)
    @Transactional
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        Post post = findPost(id);
        checkWriter(post);
        postRepository.delete(post);
        return ResponseEntity.noContent().build();
    }

    /** 없는 글은 바로 404로 응답해 이후 코드가 null을 사용하지 않게 한다. */
    private Post findPost(Long id) {
        return postRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다. id=" + id)
        );
    }

    /** JWT 필터가 SecurityContext에 넣은 로그인 User를 꺼낸다. */
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return user;
    }

    /** JWT의 회원 번호와 게시글 작성자 번호가 같은지 비교해 타인의 변경을 막는다. */
    private void checkWriter(Post post) {
        if (!post.getAuthor().getId().equals(currentUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자 본인만 수정하거나 삭제할 수 있습니다.");
        }
    }
}
