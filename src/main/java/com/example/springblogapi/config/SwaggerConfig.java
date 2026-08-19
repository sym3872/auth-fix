package com.example.springblogapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.TransformedResource;

/** Swagger 문서의 제목, JWT 입력 방식, 한국어 화면 도우미 연결을 설정한다. */
@Configuration
public class SwaggerConfig {

    /** Swagger UI에 JWT Bearer 토큰을 입력하는 방법을 알려 준다. */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SecureBlog API")
                        .version("1.1.0")
                        .description("JWT Access Token을 사용하는 학습용 블로그 API입니다."))
                .components(new Components().addSecuritySchemes(
                        SecurityConfig.BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }

    /**
     * Swagger 화면의 HTML에 초보자용 화면 도우미 파일을 연결한다.
     * 브라우저 자동 번역이 POST·GET을 엉뚱하게 바꾸지 못하게 하고, 한국어 화면 도우미를 붙인다.
     */
    @Bean
    public SwaggerIndexTransformer swaggerIndexTransformer(
            SwaggerUiConfigProperties swaggerUiConfig,
            SwaggerUiOAuthProperties swaggerUiOAuthProperties,
            SwaggerWelcomeCommon swaggerWelcomeCommon,
            ObjectMapperProvider objectMapperProvider
    ) {
        SwaggerIndexPageTransformer defaultTransformer = new SwaggerIndexPageTransformer(
                swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider
        );

        return (request, resource, transformerChain) -> {
            Resource transformed = defaultTransformer.transform(request, resource, transformerChain);
            if (!"index.html".equals(transformed.getFilename())) {
                return transformed;
            }

            String html = readResource(transformed)
                    .replace("<html lang=\"en\">", "<html lang=\"ko\" translate=\"no\">")
                    .replace("</head>", "<meta name=\"google\" content=\"notranslate\"></head>")
                    .replace("</body>", "<script src=\"/swagger-helper.js\"></script></body>");
            return new TransformedResource(transformed, html.getBytes(StandardCharsets.UTF_8));
        };
    }

    /** Swagger 라이브러리 안의 텍스트 파일을 UTF-8 문자열로 읽는다. */
    private static String readResource(Resource resource) throws IOException {
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
