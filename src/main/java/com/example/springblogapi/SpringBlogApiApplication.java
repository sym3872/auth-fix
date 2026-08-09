package com.example.springblogapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Boot 애플리케이션을 시작하는 가장 바깥쪽 클래스다.
 * 이 클래스와 같은 패키지 또는 하위 패키지(auth, post, config)를 자동으로 찾아 설정과 기능을 등록한다.
 */
// @SpringBootApplication은 자동 설정, 컴포넌트 탐색, 설정 클래스 기능을 한 번에 켠다.
// 기본 사용자(user)와 임시 비밀번호를 만드는 자동 설정은 JWT 로그인 예제에서 사용하지 않으므로 제외한다.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
// User와 Post 안에 넣은 Repository 인터페이스도 Spring Data JPA가 찾게 한다.
@EnableJpaRepositories(considerNestedRepositories = true)
public class SpringBlogApiApplication {

	/**
	 * 실행 버튼을 누르면 Spring Boot 서버를 시작한 뒤 같은 콘솔에서 숫자 메뉴를 실행한다.
	 * 메뉴에서 0번을 선택하면 메뉴와 서버를 함께 안전하게 종료한다.
	 */
	public static void main(String[] args) {
		// args에는 실행할 때 전달한 --server.port 같은 선택 설정이 들어올 수 있다.
		// 먼저 REST API와 H2 데이터베이스가 동작하도록 Spring Boot 서버를 시작한다.
		ConfigurableApplicationContext context = SpringApplication.run(
				SpringBlogApiApplication.class,
				args
		);

		try {
			// 서버 시작이 끝난 다음 같은 실행 콘솔에 숫자 메뉴를 표시한다.
			new TerminalMenu().run();
		} finally {
			// finally는 메뉴에서 오류가 나더라도 항상 실행되어 서버 자원을 정리한다.
			// 0번 종료 또는 콘솔 종료 시 서버와 데이터베이스 연결도 함께 정리한다.
			SpringApplication.exit(context);
		}
	}

}
