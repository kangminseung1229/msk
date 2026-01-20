package ai.langgraph4j.msk.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.google.genai.Client;

/**
 * Spring AI 설정 클래스
 * ChatModel과 ChatClient를 Bean으로 등록합니다.
 */
@Configuration
@org.springframework.context.annotation.Profile("!test")
public class AiConfig {

	@Value("${spring.ai.google.genai.api-key:}")
	private String apiKey;

	@Value("${spring.ai.google.genai.chat.options.model:gemini-1.5-flash}")
	private String model;

	@Value("${spring.ai.google.genai.chat.options.temperature:0.7}")
	private Double temperature;

	/**
	 * Google GenAI Client Bean 생성
	 */
	@Bean
	@ConditionalOnMissingBean(Client.class)
	@ConditionalOnProperty(name = "spring.ai.google.genai.api-key", matchIfMissing = false)
	public Client googleGenAiClient() {
		// 환경변수나 시스템 프로퍼티에서 직접 읽기 시도
		String key = apiKey;
		if (key == null || key.isEmpty() || key.trim().isEmpty()) {
			key = System.getProperty("spring.ai.google.genai.api-key");
		}
		if (key == null || key.isEmpty() || key.trim().isEmpty()) {
			key = System.getenv("GEMINI_API_KEY");
		}
		if (key == null || key.isEmpty() || key.trim().isEmpty()) {
			throw new IllegalStateException(
					"Google GenAI API key is not configured. Please set spring.ai.google.genai.api-key or GEMINI_API_KEY environment variable.");
		}
		return Client.builder()
				.apiKey(key)
				.build();
	}

	/**
	 * ChatModel Bean 생성
	 * Spring AI 자동 설정이 작동하지 않을 경우를 대비하여 명시적으로 생성합니다.
	 * 
	 * @Primary를 사용하여 자동 설정의 Bean보다 우선순위를 가집니다.
	 */
	@Bean
	@Primary
	@ConditionalOnBean(Client.class)
	public ChatModel chatModel(Client genAiClient) {
		// 모델명이 null이면 기본값 사용
		String modelName = model;
		if (modelName == null || modelName.isEmpty()) {
			modelName = System.getProperty("spring.ai.google.genai.chat.options.model", "gemini-1.5-flash");
		}
		if (modelName == null || modelName.isEmpty()) {
			modelName = "gemini-1.5-flash"; // 최종 기본값 (무료 티어에서 사용 가능)
		}

		// Temperature가 null이면 기본값 사용
		Double temp = temperature;
		if (temp == null) {
			temp = 0.7;
		}

		GoogleGenAiChatOptions chatOptions = GoogleGenAiChatOptions.builder()
				.model(modelName)
				.temperature(temp)
				.build();

		return GoogleGenAiChatModel.builder()
				.genAiClient(genAiClient)
				.defaultOptions(chatOptions)
				.build();
	}

	/**
	 * ChatClient Bean 생성
	 * LangGraph4j에서 사용할 ChatClient를 제공합니다.
	 */
	@Bean
	@ConditionalOnBean(ChatModel.class)
	public ChatClient chatClient(ChatModel chatModel) {
		return ChatClient.builder(chatModel)
				.defaultSystem("You are a helpful AI assistant.")
				.build();
	}
}
