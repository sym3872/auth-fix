# SecureBlog API - 초보자용 간소화 버전

Spring Boot, Spring Security, JWT를 처음 배우는 사람을 위한 아주 작은 백엔드 예제입니다.

- 회원가입과 로그인
- Access Token만 사용하는 JWT 인증
- 로그인한 회원만 게시글 작성
- 작성자 본인만 게시글 수정·삭제
- 설치 없이 실행되는 H2 in-memory 데이터베이스

모든 Java 클래스와 주요 메서드에는 왜 필요한 코드인지 설명하는 한글 주석을 넣었습니다. Lombok, Refresh Token, Redis, OAuth, 전역 예외 처리 클래스는 사용하지 않았습니다.

이 버전은 관련 코드를 같은 파일에 묶었습니다. 요청·응답 DTO는 Controller 안에, Repository는 Entity 안에, JWT 발급기와 검증 필터는 `SecurityConfig` 안에 두어 실행 코드 Java 파일을 7개로 줄였습니다.

## 초보자용 코드 읽는 순서

처음부터 모든 파일을 동시에 보지 말고 아래 순서로 읽으면 흐름을 이해하기 쉽습니다.

1. `SpringBlogApiApplication` — 서버와 터미널 메뉴가 시작되는 지점
2. `User`, `Post` — 엔티티와 파일 아래쪽 Repository 확인
3. `AuthController` — 회원가입, BCrypt 암호화, 로그인 흐름 확인
4. `SecurityConfig` — URL 권한 → JWT 발급기 → JWT 인증 필터 순서로 확인
5. `PostController` — 로그인 사용자와 작성자를 비교하는 CRUD 과정
6. `TerminalMenu` — 위 REST API를 숫자 메뉴에서 호출하는 과정

각 파일은 클래스 설명 → 필드 설명 → 생성자 → 메서드 순서로 읽으면 됩니다. 주석을 먼저 읽고 바로 아래 한두 줄의 코드를 확인하는 방식으로 학습하세요.

## 패키지 구조

```text
com.example.springblogapi
├── SpringBlogApiApplication.java  # 서버와 메뉴 시작
├── TerminalMenu.java              # 터미널 숫자 메뉴
├── auth
│   ├── User.java                  # User 엔티티 + UserRepository
│   └── AuthController.java        # 회원가입·로그인 + 요청·응답 record
├── post
│   ├── Post.java                  # Post 엔티티 + PostRepository
│   └── PostController.java        # 게시글 CRUD + 요청·응답 record
└── config
    └── SecurityConfig.java        # Security 설정 + JWT 발급기 + JWT 필터
```

## 실행 방법

Java 21이 설치되어 있으면 별도의 Gradle 설치가 필요 없습니다. 프로젝트에 포함된 Gradle Wrapper를 사용합니다.

```bash
cd spring-blog-api-simple
./run.sh
```

`run.sh`는 이전에 실행한 SecureBlog 서버가 8080 포트를 사용 중이면 그 서버만 먼저 종료하고 다시 실행합니다.
따라서 실수로 실행 버튼을 두 번 눌러도 `Port 8080 was already in use` 오류가 발생하지 않습니다.

서버가 시작되면 `http://localhost:8080`에서 동작합니다. 종료하려면 실행 중인 터미널에서 `Ctrl + C`를 누릅니다.

## 실행 버튼으로 숫자 메뉴 사용하기

가장 쉬운 방법은 압축을 푼 폴더의 `SecureBlog.code-workspace` 파일을 더블클릭하는 것입니다.
이 파일이 VS Code에서 Java 파일 하나가 아니라 Gradle 프로젝트 전체를 열어 줍니다.

VS Code에서는 반드시 `spring-blog-api-simple` 폴더 자체를 `File → Open Folder`로 엽니다.
그다음 `SpringBlogApiApplication.java`를 열고 오른쪽 위의 `Run Code` 실행 버튼을 누릅니다.
프로젝트에 포함한 `.vscode/settings.json`이 단일 파일 컴파일 대신 Gradle 전체 프로젝트를 실행합니다.

IntelliJ에서는 `SpringBlogApiApplication.java`를 열고 `main` 메서드 옆의 실행 버튼을 누릅니다.
Spring Boot 서버가 시작된 다음 같은 실행 콘솔에 숫자 메뉴가 자동으로 표시됩니다.

터미널 명령으로 실행하고 싶을 때도 명령은 하나뿐입니다.

```bash
cd spring-blog-api-simple
./gradlew bootRun
```

메뉴에서 `1번 회원가입 → 2번 로그인 → 5번 게시글 작성 → 3번 목록 조회` 순서로 선택하면 됩니다.
로그인 성공 시 JWT는 메뉴 프로그램이 자동으로 저장하므로 직접 복사할 필요가 없습니다.
`0번 종료`를 선택하면 메뉴와 Spring Boot 서버가 함께 종료됩니다.

테스트만 실행하려면 다음 명령을 사용합니다.

```bash
./gradlew test
```

## H2 데이터베이스 콘솔

서버가 실행 중일 때 브라우저에서 `http://localhost:8080/h2-console`에 접속할 수 있습니다.

| 항목 | 값 |
| --- | --- |
| JDBC URL | `jdbc:h2:mem:blogdb` |
| User Name | `sa` |
| Password | 비워 둠 |

H2는 메모리 데이터베이스이므로 서버를 종료하면 회원과 게시글 데이터가 모두 사라집니다. 이것은 학습 프로젝트에서는 정상 동작입니다.

## API 테스트 순서

### 1. 회원가입

`POST /api/auth/signup`

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"pass1234","nickname":"앨리스"}'
```

### 2. 로그인해서 JWT 받기

`POST /api/auth/login`

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"pass1234"}'
```

성공 응답의 `token` 값을 복사해 아래 `ACCESS_TOKEN` 자리에 넣습니다.

```bash
ACCESS_TOKEN='로그인_응답의_token_값'
```

### 3. 게시글 작성 (로그인 필요)

`POST /api/posts`

```bash
curl -X POST http://localhost:8080/api/posts \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"title":"첫 번째 글","content":"JWT로 작성한 첫 번째 게시글입니다."}'
```

### 4. 게시글 목록과 상세 조회 (로그인 불필요)

```bash
curl http://localhost:8080/api/posts
curl http://localhost:8080/api/posts/1
```

### 5. 게시글 수정 (작성자만 가능)

`PUT /api/posts/1`

```bash
curl -X PUT http://localhost:8080/api/posts/1 \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"title":"수정한 글","content":"작성자 본인만 수정할 수 있습니다."}'
```

### 6. 게시글 삭제 (작성자만 가능)

`DELETE /api/posts/1`

```bash
curl -X DELETE http://localhost:8080/api/posts/1 \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

## 인증 흐름

1. 회원가입 시 `BCryptPasswordEncoder`가 비밀번호를 암호화해 H2에 저장합니다.
2. 로그인 시 암호화된 비밀번호를 비교하고, 성공하면 이메일이 들어 있는 JWT Access Token을 반환합니다.
3. 이후 요청은 `Authorization: Bearer 토큰값` 헤더를 보냅니다.
4. `JwtAuthenticationFilter`가 토큰의 서명과 만료 시간을 검사하고 로그인한 `User`를 `SecurityContext`에 넣습니다.
5. `PostController`는 그 `User`와 게시글 작성자를 비교해 수정·삭제 권한을 확인합니다.

## 학습용 설정 주의사항

- `application.yml`의 JWT 비밀키는 바로 실행해 보기 위한 예시입니다. 실제 서비스에서는 환경 변수 또는 별도 비밀 관리 도구로 옮겨야 합니다.
- 이 예제는 Access Token만 사용하므로 로그아웃, Refresh Token, Redis, OAuth는 구현하지 않았습니다.
- 서비스가 커지면 Controller에 있는 데이터베이스 코드를 Service 클래스로 분리하는 것이 좋지만, 이 프로젝트는 흐름을 쉽게 보기 위해 생략했습니다.
