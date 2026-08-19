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
import tools.jackson.databind.ObjectMapper;

/**
 * Postman 대신 숫자를 입력해 API를 연습할 수 있는 터미널 메뉴다.
 * 실제 REST API를 호출하므로 JWT 보안과 작성자 검사를 똑같이 거친다.
 */
public class TerminalMenu {

    private final String serverUrl;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final Scanner scanner = new Scanner(System.in);
    private String token;
    private String loginEmail;

    /** 실제 서버 주소를 받아 메뉴의 API 요청과 Swagger 주소가 항상 같게 만든다. */
    public TerminalMenu(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /** 사용자가 0번을 선택할 때까지 메뉴를 반복한다. */
    public void run() {
        System.out.println("\nSpring Boot 서버와 터미널 메뉴가 실행되었습니다.");
        System.out.println("Swagger 문서 주소: " + serverUrl + "/swagger-ui.html");

        while (true) {
            printMenu();
            try {
                switch (input("메뉴 번호: ")) {
                    case "1" -> signup();
                    case "2" -> login();
                    case "3" -> show("게시글 목록", request("GET", "/api/posts", null, false));
                    case "4" -> show("게시글 상세", request("GET", "/api/posts/" + postId(), null, false));
                    case "5" -> createPost();
                    case "6" -> updatePost();
                    case "7" -> deletePost();
                    case "8" -> logout();
                    case "0" -> {
                        System.out.println("터미널 메뉴를 닫습니다. 서버를 종료하려면 Ctrl+C를 누르세요.");
                        return;
                    }
                    default -> System.out.println("0~8 사이의 번호를 입력해 주세요.");
                }
            } catch (NoSuchElementException exception) {
                return;
            } catch (ConnectException exception) {
                System.out.println("서버에 연결할 수 없습니다.");
            } catch (Exception exception) {
                System.out.println("처리 실패: " + exception.getMessage());
            }
        }
    }

    /** 현재 로그인 상태와 가능한 기능을 출력한다. */
    private void printMenu() {
        String state = token == null ? "로그아웃" : loginEmail + " 로그인 중";
        System.out.printf("""

          ============================
           SecureBlog 메뉴 (%s)
           Swagger 문서: %s/swagger-ui.html
          ============================
                1. 회원가입
                2. 로그인
                3. 게시글 목록
                4. 게시글 상세
                5. 게시글 작성 (로그인 필요)
                6. 게시글 수정 (작성자만)
                7. 게시글 삭제 (작성자만)
                8. 로그아웃
                0. 종료
          """, state, serverUrl);
    }

    /** 회원가입 API를 호출한다. 입력 검증은 서버의 @Valid가 담당한다. */
    private void signup() throws IOException, InterruptedException {
        show("회원가입", request("POST", "/api/auth/signup", Map.of(
                "email", inputRequired("이메일: "),
                "password", inputRequired("비밀번호: "),
                "nickname", inputRequired("닉네임: ")
        ), false));
    }

    /** 로그인 성공 시 응답의 Access Token을 저장한다. */
    private void login() throws IOException, InterruptedException {
        String email = inputRequired("이메일: ");
        HttpResponse<String> response = request("POST", "/api/auth/login", Map.of(
                "email", email,
                "password", inputRequired("비밀번호: ")
        ), false);
        show("로그인", response);

        if (response.statusCode() == 200) {
            // 로그인 응답 본문은 JWT 한 줄뿐이므로 그대로 저장하면 된다.
            token = response.body().trim();
            loginEmail = email;
        }
    }

    /** 저장한 JWT를 넣어 게시글을 작성한다. */
    private void createPost() throws IOException, InterruptedException {
        if (checkLogin()) {
            show("게시글 작성", request("POST", "/api/posts", postBody("제목: ", "내용: "), true));
        }
    }

    /** 저장한 JWT를 넣어 작성자 본인의 게시글을 수정한다. */
    private void updatePost() throws IOException, InterruptedException {
        if (checkLogin()) {
            String id = postId();
            show("게시글 수정", request("PUT", "/api/posts/" + id, postBody("새 제목: ", "새 내용: "), true));
        }
    }

    /** 저장한 JWT를 넣어 작성자 본인의 게시글을 삭제한다. */
    private void deletePost() throws IOException, InterruptedException {
        if (!checkLogin()) {
            return;
        }

        String id = postId();
        if (input("정말 삭제할까요? (y/N): ").equalsIgnoreCase("y")) {
            show("게시글 삭제", request("DELETE", "/api/posts/" + id, null, true));
        }
    }

    /** 작성과 수정에서 공통으로 쓰는 title, content JSON을 만든다. */
    private Map<String, String> postBody(String titleMessage, String contentMessage) {
        return Map.of("title", inputRequired(titleMessage), "content", inputRequired(contentMessage));
    }

    /** 게시글 번호는 1 이상의 숫자만 받아 서버 오류 대신 바로 입력 안내를 보여 준다. */
    private String postId() {
        while (true) {
            String id = inputRequired("게시글 번호: ");
            if (id.matches("[1-9]\\d*")) {
                return id;
            }
            System.out.println("게시글 번호는 1 이상의 숫자로 입력해 주세요.");
        }
    }

    /** 서버가 세션을 저장하지 않으므로 메뉴가 가진 토큰을 지우면 로그아웃이다. */
    private void logout() {
        token = null;
        loginEmail = null;
        System.out.println("로그아웃되었습니다.");
    }

    /** 보호 API는 JWT가 있을 때만 메뉴에서 요청한다. */
    private boolean checkLogin() {
        if (token != null) {
            return true;
        }
        System.out.println("먼저 2번 메뉴에서 로그인해 주세요.");
        return false;
    }

    /** HTTP 메서드, 주소, JSON 본문, JWT 포함 여부로 API 요청을 만든다. */
    private HttpResponse<String> request(
            String method, String path, Map<String, String> body, boolean useToken
    ) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                // 로그인은 JWT 문자열(text/plain)을, 나머지 API는 JSON을 반환할 수 있다.
                .header("Accept", "application/json, text/plain");

        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        if (useToken) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body));

        return http.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString());
    }

    /** 상태 코드와 서버 JSON을 그대로 출력해 API 응답을 쉽게 확인한다. */
    private void show(String name, HttpResponse<String> response) {
        String body = response.body().isBlank() ? "완료" : response.body();
        System.out.printf("\n[%s] HTTP %d%n%s%n", name, response.statusCode(), body);
    }

    /** 한 줄 입력을 받고, 빈 값은 다시 입력하게 한다. */
    private String input(String message) {
        System.out.print(message);
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException();
        }
        return scanner.nextLine().trim();
    }

    /** 빈 문자열을 막아 회원가입·로그인·게시글 입력을 조금 더 친절하게 만든다. */
    private String inputRequired(String message) {
        while (true) {
            String value = input(message);
            if (!value.isBlank()) {
                return value;
            }
            System.out.println("값을 비워 둘 수 없습니다.");
        }
    }
}
