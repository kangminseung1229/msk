package ai.langgraph4j.msk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot 애플리케이션 컨텍스트 로딩 테스트
 * test 프로파일에서는 AiConfig가 @Profile("!test")로 인해 제외됩니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class MskApplicationTests {

	@Test
	void contextLoads() {
		// 컨텍스트가 정상적으로 로드되는지 확인
	}

}
