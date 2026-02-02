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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.google.genai.Client;

import ai.langgraph4j.aiagent.repository.TaxLawCodeRepository;
import ai.langgraph4j.aiagent.service.ChatSessionPersistenceService;
import ai.langgraph4j.aiagent.service.ConsultationSearchService;
import ai.langgraph4j.aiagent.service.CounselEmbeddingService;
import ai.langgraph4j.aiagent.service.LawArticleEmbeddingService;
import ai.langgraph4j.aiagent.service.YpEmbeddingService;

/**
 * Spring Boot 애플리케이션 컨텍스트 로딩 테스트
 * test 프로파일에서는 AiConfig와 ToolConfig가 @Profile("!test")로 인해 제외됩니다.
 * JPA/DataSource/PgVectorStore는 application-test.properties에서 제외되므로,
 * 이에 의존하는 서비스·리포지토리는 @MockBean으로 대체합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AiagentApplicationTests {

	@MockBean
	CounselEmbeddingService counselEmbeddingService;

	@MockBean
	LawArticleEmbeddingService lawArticleEmbeddingService;

	@MockBean
	ConsultationSearchService consultationSearchService;

	@MockBean
	TaxLawCodeRepository taxLawCodeRepository;

	@MockBean
	YpEmbeddingService ypEmbeddingService;

	@MockBean
	ChatSessionPersistenceService chatSessionPersistenceService;

	@TestConfiguration(proxyBeanMethods = false)
	static class TestConfig {
		@Bean
		@Primary
		@Qualifier("chatModel")
		static ChatModel chatModel() {
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
		static Client googleGenAiClient() {
			return mock(Client.class);
		}
	}

	@Test
	void contextLoads() {
		// 컨텍스트가 정상적으로 로드되는지 확인
	}
}
