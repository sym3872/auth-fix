package com.example.springblogapi.auth;

import com.example.springblogapi.config.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 회원가입과 로그인 API의 요청 흐름을 담당하는 컨트롤러다. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "회원가입과 로그인 API")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /** Spring이 회원 저장소, BCrypt 암호화기, JWT 도구를 생성자로 넣어 준다. */
    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** 이메일 중복을 확인하고 BCrypt로 암호화한 비밀번호를 저장한다. */
    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "입력값이 올바르지 않음"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    })
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        User savedUser = userRepository.save(new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        ));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignupResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getNickname()));
    }

    /** 이메일과 BCrypt 비밀번호를 비교하고, 성공하면 JWT 문자열만 반환한다. */
    @PostMapping(value = "/login", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
            summary = "로그인",
            description = "성공 응답에 표시되는 JWT 한 줄 전체만 복사하세요. 오른쪽 위 토큰 입력 버튼에는 "
                    + "토큰만 붙여넣으면 Bearer는 Swagger가 자동으로 붙입니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 - 본문 전체가 JWT Access Token입니다.",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiJ9..."))
            ),
            @ApiResponse(responseCode = "400", description = "입력값이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호가 올바르지 않음")
    })
    public String login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElseThrow(this::loginFailed);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw loginFailed();
        }

        return jwtTokenProvider.createToken(user.getEmail());
    }

    /** 이메일과 비밀번호 중 무엇이 틀렸는지 노출하지 않는 로그인 실패 응답이다. */
    private ResponseStatusException loginFailed() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
