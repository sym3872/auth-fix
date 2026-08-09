package com.example.springblogapi;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 숫자 메뉴에서 기존 REST API를 호출하는 간단한 터미널 클라이언트다.
 * Postman 대신 사용할 뿐이며, 요청은 실제 Spring Security와 JWT 검사를 그대로 거친다.
 */
public class TerminalMenu {

    /** 메뉴가 요청을 보낼 현재 컴퓨터의 Spring Boot 서버 주소다. */
    private static final String SERVER_URL = "http://localhost:8080";

    /** Java가 제공하는 HTTP 클라이언트다. curl처럼 서버에 HTTP 요청을 보낸다. */
    private final HttpClient http = HttpClient.newHttpClient();

    /** 자바의 Map과 서버의 JSON 문자열을 서로 바꾸는 Jackson 도구다. */
    private final ObjectMapper json = new ObjectMapper();

    /** System.in, 즉 실행 콘솔에서 사용자가 입력한 한 줄을 읽는 도구다. */
    private final Scanner scanner = new Scanner(System.in);

    /** 로그인 성공 후 받은 JWT를 프로그램이 종료될 때까지 메모리에 보관한다. */
    private String token;

    /** 메뉴 윗부분에 현재 로그인한 회원을 표시하기 위한 이메일이다. */
    private String loginEmail;

    /** 0번을 선택할 때까지 메뉴를 반복한다. */
    public void run() {
        // SpringBlogApiApplication이 서버 시작을 마친 다음 이 문장을 출력한다.
        System.out.println("\nSpring Boot 서버와 터미널 메뉴가 실행되었습니다.");

        // 사용자가 0번을 누르기 전까지 같은 메뉴를 계속 보여 준다.
        while (true) {
            // 선택 가능한 기능과 현재 로그인 상태를 먼저 출력한다.
            printMenu();

            try {
                // 사용자가 입력한 번호에 맞는 메서드 하나를 실행한다.
                switch (input("메뉴 번호: ")) {
                    case "1" -> signup();
                    case "2" -> login();
                    case "3" -> listPosts();
                    case "4" -> detailPost();
                    case "5" -> createPost();
                    case "6" -> updatePost();
                    case "7" -> deletePost();
                    case "8" -> logout();
                    case "0" -> {
                        // return을 만나면 run 메서드가 끝나고 메인 클래스가 서버도 종료한다.
                        System.out.println("프로그램을 종료합니다.");
                        return;
                    }
                    // 정해진 메뉴 번호가 아니면 기능을 실행하지 않고 다시 메뉴를 보여 준다.
                    default -> System.out.println("0~8 사이의 번호를 입력해 주세요.");
                }
            } catch (NoSuchElementException exception) {
                // 실행 콘솔이 닫혀 더 이상 입력을 받을 수 없을 때 반복문을 끝낸다.
                return;
            } catch (ConnectException exception) {
                // 8080번 포트의 Spring Boot 서버와 연결하지 못한 경우다.
                System.out.println("서버에 연결할 수 없습니다.");
            } catch (Exception exception) {
                // 잘못된 JSON이나 통신 문제 등 나머지 오류를 메뉴가 갑자기 종료되지 않게 보여 준다.
                System.out.println("처리 실패: " + exception.getMessage());
            }
        }
    }

    /** 현재 로그인 상태와 기능 목록을 보여 준다. */
    private void printMenu() {
        // token이 없으면 로그아웃, 있으면 저장한 이메일을 로그인 상태로 표시한다.
        String state = token == null ? "로그아웃" : loginEmail + " 로그인 중";

        // 아래 출력문들은 사용자가 선택할 수 있는 숫자 메뉴를 만든다.
        System.out.println("\n============================");
        System.out.println(" SecureBlog 메뉴 (" + state + ")");
        System.out.println("============================");
        System.out.println("1. 회원가입");
        System.out.println("2. 로그인");
        System.out.println("3. 게시글 목록");
        System.out.println("4. 게시글 상세");
        System.out.println("5. 게시글 작성 (로그인 필요)");
        System.out.println("6. 게시글 수정 (작성자만)");
        System.out.println("7. 게시글 삭제 (작성자만)");
        System.out.println("8. 로그아웃");
        System.out.println("0. 종료");
    }

    /** 입력받은 회원 정보로 회원가입 API를 호출한다. */
    private void signup() throws IOException, InterruptedException {
        // Map의 key는 서버 SignupRequest의 필드 이름과 정확히 같아야 한다.
        Map<String, String> body = Map.of(
                "email", inputEmail(),
                "password", inputNewPassword(),
                "nickname", inputRequired("닉네임: ")
        );

        // 인증이 필요 없는 회원가입 주소로 POST 요청을 보내고 결과를 출력한다.
        show("회원가입", request("POST", "/api/auth/signup", body, false));
    }

    /** 로그인하고 응답으로 받은 JWT를 자동 저장한다. */
    private void login() throws IOException, InterruptedException {
        // 입력한 이메일은 요청에도 쓰고 로그인 상태 표시에도 쓰므로 변수에 보관한다.
        String email = inputEmail();

        // 로그인 API가 요구하는 email과 password를 JSON으로 만들기 위한 Map이다.
        Map<String, String> body = Map.of(
                "email", email,
                "password", inputRequired("비밀번호: ")
        );

        // 로그인은 아직 JWT가 없으므로 마지막 인자를 false로 보내 POST 요청을 만든다.
        HttpResponse<String> response = request("POST", "/api/auth/login", body, false);

        // 서버 응답을 JSON 대신 사람이 읽기 쉬운 형태로 출력한다.
        show("로그인", response);

        // HTTP 200은 이메일과 비밀번호가 맞아 로그인에 성공했다는 뜻이다.
        if (response.statusCode() == 200) {
            // JSON 문자열을 JsonNode로 바꾸면 token 값을 이름으로 꺼낼 수 있다.
            JsonNode result = json.readTree(response.body());

            // 이후 글 작성·수정·삭제 요청의 Authorization 헤더에 넣을 JWT다.
            token = result.path("token").asString();

            // 메뉴에 누구로 로그인했는지 표시하기 위해 이메일도 함께 기억한다.
            loginEmail = email;

            // 사용자에게는 복잡한 JWT 원문을 보여 주지 않고 로그인 상태만 안내한다.
            System.out.println("로그인 상태가 안전하게 저장되었습니다.");
        }
    }

    /** 공개 API로 모든 게시글을 조회한다. */
    private void listPosts() throws IOException, InterruptedException {
        // GET 목록 조회는 공개 API라 요청 본문과 JWT가 모두 필요 없다.
        show("게시글 목록", request("GET", "/api/posts", null, false));
    }

    /** 글 번호를 입력받아 게시글 한 건을 조회한다. */
    private void detailPost() throws IOException, InterruptedException {
        // 조회할 게시글 id를 문자열로 받아 URL 마지막에 붙인다.
        String id = inputPostId();

        // 예를 들어 id가 1이면 GET /api/posts/1 요청이 된다.
        show("게시글 상세", request("GET", "/api/posts/" + id, null, false));
    }

    /** 로그인한 회원 이름으로 게시글을 작성한다. */
    private void createPost() throws IOException, InterruptedException {
        // JWT가 없으면 보호 API를 호출하지 않고 메뉴로 돌아간다.
        if (!checkLogin()) {
            return;
        }

        // PostController의 CreatePostRequest가 요구하는 title과 content를 만든다.
        Map<String, String> body = Map.of(
                "title", inputRequired("제목: "),
                "content", inputRequired("내용: ")
        );

        // 마지막 인자가 true이므로 request 메서드가 JWT를 헤더에 넣는다.
        show("게시글 작성", request("POST", "/api/posts", body, true));
    }

    /** 작성자 본인의 게시글 제목과 내용을 수정한다. */
    private void updatePost() throws IOException, InterruptedException {
        // 수정은 로그인한 사용자만 가능하므로 먼저 JWT가 있는지 확인한다.
        if (!checkLogin()) {
            return;
        }

        // 어떤 게시글을 수정할지 id를 입력받는다.
        String id = inputPostId();

        // 변경할 제목과 내용을 PUT 요청의 JSON 본문으로 만든다.
        Map<String, String> body = Map.of(
                "title", inputRequired("새 제목: "),
                "content", inputRequired("새 내용: ")
        );

        // 서버는 JWT의 회원과 게시글 작성자가 같은지도 추가로 검사한다.
        show("게시글 수정", request("PUT", "/api/posts/" + id, body, true));
    }

    /** 작성자 본인의 게시글을 삭제한다. */
    private void deletePost() throws IOException, InterruptedException {
        // 삭제도 보호 API이므로 로그인하지 않았다면 요청하지 않는다.
        if (!checkLogin()) {
            return;
        }

        // 삭제할 게시글 번호를 URL에 사용한다.
        String id = inputPostId();

        // 실수로 삭제하는 일을 줄이기 위해 y를 입력했을 때만 계속한다.
        if (!input("정말 삭제할까요? (y/N): ").equalsIgnoreCase("y")) {
            System.out.println("삭제를 취소했습니다.");
            return;
        }

        // DELETE 요청은 JSON 본문이 없지만 작성자 확인을 위한 JWT는 보낸다.
        show("게시글 삭제", request("DELETE", "/api/posts/" + id, null, true));
    }

    /** 저장했던 JWT를 지워 로그아웃 상태로 만든다. */
    private void logout() {
        // 서버 세션을 사용하지 않으므로 클라이언트가 가진 JWT를 지우면 로그아웃이다.
        token = null;

        // 로그인 상태 표시에 사용하던 이메일도 함께 지운다.
        loginEmail = null;

        System.out.println("로그아웃되었습니다.");
    }

    /** 로그인이 필요한 메뉴를 로그인 전에 사용하지 못하게 안내한다. */
    private boolean checkLogin() {
        // 아직 로그인하지 않았다면 token 필드는 null이다.
        if (token == null) {
            System.out.println("먼저 2번 메뉴에서 로그인해 주세요.");
            return false;
        }

        // token이 있으면 보호 API 요청을 계속 진행해도 된다는 뜻이다.
        return true;
    }

    /** HTTP 요청을 만들고 서버가 보낸 상태 코드와 JSON을 받는다. */
    private HttpResponse<String> request(
            String method,
            String path,
            Map<String, String> body,
            boolean useToken
    ) throws IOException, InterruptedException {
        // 요청 주소와 서버가 받고 싶은 응답 형식(JSON)을 기본 설정한다.
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + path))
                .header("Accept", "application/json");

        // 글 작성·수정·삭제 요청이면 저장해 둔 JWT를 Bearer 방식으로 보낸다.
        if (useToken) {
            builder.header("Authorization", "Bearer " + token);
        }

        // GET·DELETE처럼 본문이 없는 요청은 빈 BodyPublisher로 시작한다.
        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.noBody();

        // 회원가입·로그인·작성·수정처럼 body Map이 있으면 JSON 문자열로 변환한다.
        if (body != null) {
            // 서버에 본문이 JSON 형식임을 Content-Type 헤더로 알려 준다.
            builder.header("Content-Type", "application/json");

            // Map을 JSON 문자열로 바꾸고 실제 HTTP 요청 본문에 넣는다.
            publisher = HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body));
        }

        // 전달받은 GET, POST, PUT, DELETE 메서드와 본문을 합쳐 최종 요청을 만든다.
        HttpRequest request = builder.method(method, publisher).build();

        // 요청을 서버에 보내고 응답 본문을 String으로 받아 반환한다.
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** 서버의 JSON 응답을 메뉴별로 정리해 읽기 쉬운 한글로 출력한다. */
    private void show(String name, HttpResponse<String> response) {
        // 오류가 발생하면 HTTP 번호나 JSON 대신 이해하기 쉬운 문장을 보여 준다.
        if (response.statusCode() >= 400) {
            String message = switch (response.statusCode()) {
                case 400 -> "입력 형식이 올바르지 않습니다. 안내에 맞게 다시 입력해 주세요.";
                case 401 -> "이메일·비밀번호가 틀렸거나 로그인이 필요합니다.";
                case 403 -> "작성자 본인만 수정하거나 삭제할 수 있습니다.";
                case 404 -> "해당 번호의 게시글을 찾을 수 없습니다.";
                case 409 -> "이미 가입된 이메일입니다.";
                default -> "요청을 처리하지 못했습니다.";
            };
            System.out.println("\n============================");
            System.out.println(" " + name + " 실패");
            System.out.println("============================");
            System.out.println(message);
            return;
        }

        // 삭제 성공은 서버가 본문을 보내지 않으므로 완료 문구를 직접 출력한다.
        if (response.body().isBlank()) {
            System.out.println("\n============================");
            System.out.println(" 게시글 삭제 완료");
            System.out.println("============================");
            System.out.println("게시글이 정상적으로 삭제되었습니다.");
            return;
        }

        try {
            // JSON을 JsonNode로 읽으면 id, title 같은 필요한 값만 꺼낼 수 있다.
            JsonNode result = json.readTree(response.body());

            // 실행한 메뉴에 따라 회원 또는 게시글 출력 방식을 선택한다.
            switch (name) {
                case "회원가입" -> printMember("회원가입 완료", result);
                case "로그인" -> printMember("로그인 성공", result);
                case "게시글 목록" -> printPostList(result);
                case "게시글 상세" -> printPost("게시글 상세", result);
                case "게시글 작성" -> printPost("게시글 작성 완료", result);
                case "게시글 수정" -> printPost("게시글 수정 완료", result);
                default -> System.out.println(name + "이(가) 완료되었습니다.");
            }
        } catch (Exception exception) {
            // 예상하지 못한 응답이 와도 복잡한 원문을 노출하지 않고 안내만 한다.
            System.out.println("\n결과를 표시하지 못했습니다. 메뉴를 다시 실행해 주세요.");
        }
    }

    /** 회원가입과 로그인 결과에서 필요한 회원 정보만 출력한다. */
    private void printMember(String title, JsonNode member) {
        // 제목을 구분선 사이에 넣어 어떤 결과인지 쉽게 보이게 한다.
        System.out.println("\n============================");
        System.out.println(" " + title);
        System.out.println("============================");
        System.out.println("회원 번호 : " + member.path("id").asString());
        System.out.println("이메일    : " + member.path("email").asString());
        System.out.println("닉네임    : " + member.path("nickname").asString());
    }

    /** 게시글 한 건을 글 번호, 제목, 내용, 작성자 순으로 출력한다. */
    private void printPost(String title, JsonNode post) {
        // JSON의 괄호와 필드명 대신 한글 표를 보여 준다.
        System.out.println("\n============================");
        System.out.println(" " + title);
        System.out.println("============================");
        System.out.println("글 번호 : " + post.path("id").asString());
        System.out.println("제목    : " + post.path("title").asString());
        System.out.println("내용    : " + post.path("content").asString());
        System.out.println("작성자  : " + post.path("authorNickname").asString());
    }

    /** 게시글 목록을 배열 JSON 대신 글별 카드 형태로 출력한다. */
    private void printPostList(JsonNode posts) {
        // 목록 제목을 먼저 출력한다.
        System.out.println("\n============================");
        System.out.println(" 게시글 목록");
        System.out.println("============================");

        // 게시글이 하나도 없을 때는 빈 괄호가 아닌 안내 문구를 보여 준다.
        if (posts.isEmpty()) {
            System.out.println("아직 작성된 게시글이 없습니다.");
            return;
        }

        // 배열에 들어 있는 게시글을 하나씩 꺼내어 읽기 좋게 나눠 출력한다.
        for (JsonNode post : posts) {
            System.out.println("[" + post.path("id").asString() + "번 글]");
            System.out.println("제목   : " + post.path("title").asString());
            System.out.println("내용   : " + post.path("content").asString());
            System.out.println("작성자 : " + post.path("authorNickname").asString());
            System.out.println("----------------------------");
        }

        // 마지막에 전체 게시글 수를 알려 준다.
        System.out.println("총 " + posts.size() + "개의 게시글이 있습니다.");
    }

    /** 안내 문구를 보여 주고 사용자의 한 줄 입력을 읽는다. */
    private String input(String message) {
        // print를 사용하면 사용자가 같은 줄에서 값을 입력할 수 있다.
        System.out.print(message);

        // 실행 콘솔이 닫혀 다음 줄이 없다면 run 메서드가 종료하도록 예외를 만든다.
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException();
        }

        // 앞뒤 공백을 제거한 실제 입력값을 호출한 메서드에 돌려준다.
        return scanner.nextLine().trim();
    }

    /** 필수 입력을 비워 두면 같은 항목을 다시 입력받는다. */
    private String inputRequired(String message) {
        while (true) {
            String value = input(message);
            if (!value.isBlank()) {
                return value;
            }
            System.out.println("값을 비워 둘 수 없습니다.");
        }
    }

    /** 이메일 형식이 맞을 때까지 올바른 예시와 함께 다시 입력받는다. */
    private String inputEmail() {
        while (true) {
            String email = inputRequired("이메일: ");
            if (email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                return email;
            }
            System.out.println("이메일 형식이 아닙니다. 예: test@example.com");
        }
    }

    /** 회원가입 비밀번호가 네 글자 이상이 될 때까지 다시 입력받는다. */
    private String inputNewPassword() {
        while (true) {
            String password = inputRequired("비밀번호(4글자 이상): ");
            if (password.length() >= 4) {
                return password;
            }
            System.out.println("비밀번호는 4글자 이상 입력해 주세요.");
        }
    }

    /** 게시글 번호가 1 이상의 숫자인지 확인한 뒤 문자열로 반환한다. */
    private String inputPostId() {
        while (true) {
            String id = inputRequired("게시글 번호: ");
            if (id.matches("[1-9][0-9]*")) {
                return id;
            }
            System.out.println("게시글 번호는 1 이상의 숫자로 입력해 주세요.");
        }
    }
}
