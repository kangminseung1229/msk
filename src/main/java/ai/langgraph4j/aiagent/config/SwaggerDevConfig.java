package ai.langgraph4j.aiagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * SwaggerConfig rest api 표준 정의서 설정
 */
@Configuration
@Profile({"default"})
@OpenAPIDefinition(
        info = @Info(title = "AI Agent API", version = "1.0", description = "AI Agent API"),
        servers = {@Server(url = "http://localhost:8080", description = "Local Server")}
)
@Slf4j
public class SwaggerDevConfig {

    @PostConstruct
    void init() {
      log.info("개발 SWAGGER CONFIG 적용");
    }


}