package ai.langgraph4j.aiagent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.google.genai.Client;

/**
 * Spring Boot 애플리케이션 컨텍스트 로딩 테스트
 * test 프로파일에서는 AiConfig와 ToolConfig가 @Profile("!test")로 인해 제외됩니다.
 * 테스트용 Mock ChatModel을 제공하여 Agent 관련 컴포넌트들이 정상적으로 로드되도록 합니다.
 * application-test.properties에서 데이터베이스 자동 설정이 제외됩니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AiagentApplicationTests {

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		@Qualifier("chatModel")
		public ChatModel chatModel() {
			ChatModel mockChatModel = mock(ChatModel.class);
			ChatResponse mockResponse = mock(ChatResponse.class);
			Generation mockGeneration = mock(Generation.class);
			AssistantMessage mockMessage = new AssistantMessage("Test response");

			when(mockGeneration.getOutput()).thenReturn(mockMessage);
			when(mockResponse.getResult()).thenReturn(mockGeneration);
			when(mockChatModel.call(any(Prompt.class))).thenReturn(mockResponse);

			return mockChatModel;
		}

		@Bean
		@Primary
		public Client googleGenAiClient() {
			// 테스트용 Mock Client
			return mock(Client.class);
		}
	}

	@Test
	void contextLoads() {
		// 컨텍스트가 정상적으로 로드되는지 확인
		// Mock ChatModel이 제공되어 Agent 관련 컴포넌트들이 정상적으로 로드됩니다.
	}

}
