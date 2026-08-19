package com.example.springblogapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/** Spring Boot 서버를 시작하는 클래스다. */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SpringBlogApiApplication {

    /** 서버를 시작한 뒤 같은 터미널에서 API 연습 메뉴를 실행한다. */
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SpringBlogApiApplication.class, args);

        // application.yml 또는 실행 옵션의 포트를 읽어 실제 서버와 메뉴 주소를 맞춘다.
        String port = context.getEnvironment().getProperty("server.port", "8080");
        new TerminalMenu("http://localhost:" + port).run();
    }
}
