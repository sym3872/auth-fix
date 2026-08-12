package com.example.springblogapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI가 보여 줄 API 기본 정보와 JWT Bearer 인증 방식을 설정하는 클래스다.
 * 공개 API까지 인증 필요로 표시하지 않도록 전역 보안 요구사항은 등록하지 않는다.
 */
@Configuration
public class SwaggerConfig {

    /** 게시글 작성·수정·삭제 API가 Swagger 문서에서 참조할 JWT 인증 방식 이름이다. */
    public static final String BEARER_AUTH = "bearerAuth";

    /** OpenAPI 문서의 제목, 설명, 로컬 서버 주소, JWT 보안 스키마를 만든다. */
    @Bean
    public OpenAPI openAPI() {
        // API 문서의 제목, 버전, 사용 방법을 작성한다.
        Info info = new Info()
                .title("SecureBlog API")
                .version("1.1.0")
                .description("Spring Security와 JWT Access Token을 사용하는 학습용 블로그 API입니다. "
                        + "보호 API는 로그인 응답의 token 값을 Authorize 창에 입력한 뒤 테스트할 수 있습니다.")
                .license(new License().name("학습용 프로젝트"));

        // Swagger UI가 현재 애플리케이션을 호출할 기본 서버 주소를 등록한다.
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("로컬 개발 서버");

        // HTTP Bearer 방식이며 JWT 문자열을 사용한다는 보안 스키마를 만든다.
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("로그인 응답의 token 값만 입력하면 Bearer 접두사는 Swagger UI가 자동으로 붙습니다.");

        // 구성한 정보를 OpenAPI 객체에 담아 Springdoc이 /v3/api-docs와 Swagger UI에 사용하게 한다.
        return new OpenAPI()
                .info(info)
                .addServersItem(localServer)
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme));
    }
}
