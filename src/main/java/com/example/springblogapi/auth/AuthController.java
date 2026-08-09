package com.example.springblogapi.auth;

import com.example.springblogapi.auth.User.UserRepository;
import com.example.springblogapi.config.SecurityConfig.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 회원가입과 로그인을 처리하는 REST API 컨트롤러다.
 *
 * 회원가입에서는 비밀번호를 암호화해 저장하고, 로그인 성공 시에는 JWT Access Token을 발급한다.
 */
// @RestController는 메서드 반환값을 HTML 화면이 아니라 JSON 응답으로 보낸다.
@RestController
// 이 클래스 안의 모든 주소 앞에 /api/auth를 공통으로 붙인다.
@RequestMapping("/api/auth")
public class AuthController {

    /** 회원을 저장하거나 이메일로 찾기 위한 데이터베이스 창구다. */
    private final UserRepository userRepository;

    /** 비밀번호를 BCrypt 방식으로 암호화하고 비교하는 도구다. SecurityConfig가 Bean으로 제공한다. */
    private final PasswordEncoder passwordEncoder;

    /** 로그인 성공 시 이메일을 담은 JWT Access Token을 만드는 도구다. */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 필요한 객체를 생성자로 받아 둔다.
     * Spring이 UserRepository, PasswordEncoder, JwtTokenProvider Bean을 자동으로 넣어 준다.
     */
    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        // 생성자 인자를 같은 이름의 필드에 저장해 다른 메서드에서도 사용한다.
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * POST /api/auth/signup 요청으로 회원을 만든다.
     * 이메일 중복을 먼저 확인하고, 평문 비밀번호를 BCrypt로 암호화한 뒤에만 데이터베이스에 저장한다.
     */
    // POST /api/auth/signup 요청을 이 메서드와 연결한다.
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        // @RequestBody는 JSON을 SignupRequest로 바꾸고, @Valid는 그 안의 검증 어노테이션을 실행한다.
        // 같은 이메일로 여러 계정을 만들지 않도록 저장하기 전에 검사한다.
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        // 평문 비밀번호를 데이터베이스에 남기지 않도록 BCrypt 해시 문자열로 바꾼다.
        String encodedPassword = passwordEncoder.encode(request.password());

        // 암호화된 비밀번호와 회원가입 정보를 User 엔티티로 만들어 저장한다.
        User savedUser = userRepository.save(new User(
                request.email(),
                encodedPassword,
                request.nickname()
        ));

        // 비밀번호를 제외한 안전한 회원 정보만 201 Created 상태와 함께 반환한다.
        SignupResponse response = new SignupResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getNickname()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login 요청으로 이메일과 비밀번호가 맞는지 확인한다.
     * 둘 다 맞으면 이메일을 담은 JWT Access Token을 만들어 응답에 넣는다.
     */
    // POST /api/auth/login 요청을 이 메서드와 연결한다.
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // @Valid 덕분에 빈 이메일이나 비밀번호는 아래 로그인 로직 전에 400 오류가 된다.
        // 이메일로 회원을 찾고, 없으면 이메일과 비밀번호 어느 쪽이 틀렸는지 숨긴 채 실패시킨다.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                ));

        // 평문 요청 비밀번호와 저장된 BCrypt 해시를 안전한 matches 메서드로 비교한다.
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        // 비밀번호 자체 대신 로그인 상태를 증명할 JWT Access Token을 만든다.
        String token = jwtTokenProvider.createToken(user.getEmail());

        // 비밀번호를 제외한 회원 정보와 JWT 토큰을 클라이언트에 반환한다.
        return new LoginResponse(user.getId(), user.getEmail(), user.getNickname(), token);
    }

    /**
     * 회원가입 JSON의 이메일, 비밀번호, 닉네임을 한 번에 받는 작은 자료 상자다.
     * record를 사용하면 생성자와 값을 읽는 메서드를 Java가 자동으로 만들어 주어 별도 DTO 파일이 필요 없다.
     */
    public record SignupRequest(
            // @NotBlank는 null, 빈 문자열, 공백만 있는 문자열을 모두 거부한다.
            @NotBlank(message = "이메일은 필수입니다.")
            // @Email은 입력값이 이메일 모양인지 검사한다.
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            // 비밀번호가 비어 있는지 먼저 검사한다.
            @NotBlank(message = "비밀번호는 필수입니다.")
            // 학습 예제에서는 최소 네 글자를 요구한다.
            @Size(min = 4, message = "비밀번호는 4글자 이상이어야 합니다.")
            String password,

            // 게시글 작성자 이름으로 사용할 닉네임도 필수 값이다.
            @NotBlank(message = "닉네임은 필수입니다.")
            String nickname
    ) {
    }

    /** 회원가입 응답에서 비밀번호를 제외하고 안전한 회원 정보만 반환하는 자료 상자다. */
    public record SignupResponse(Long id, String email, String nickname) {
    }

    /** 로그인 JSON에서 이메일과 비밀번호만 받는 자료 상자다. */
    public record LoginRequest(
            // 로그인 요청에서도 이메일 형식과 빈 값을 확인한다.
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            // 비밀번호가 비어 있으면 데이터베이스를 조회할 필요 없이 요청을 거부한다.
            @NotBlank(message = "비밀번호는 필수입니다.")
            String password
    ) {
    }

    /** 로그인 성공 후 회원 정보와 JWT Access Token을 반환하는 자료 상자다. */
    public record LoginResponse(Long id, String email, String nickname, String token) {
    }
}
