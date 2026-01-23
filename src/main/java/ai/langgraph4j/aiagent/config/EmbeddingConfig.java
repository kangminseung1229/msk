package ai.langgraph4j.aiagent.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions.TaskType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.google.genai.Client;

import lombok.extern.slf4j.Slf4j;

/**
 * Embedding Model 설정
 * Gemini Embedding을 사용하여 텍스트를 벡터로 변환합니다.
 * 
 * output-dimensionality 설정이 auto-configuration에서 제대로 적용되지 않는 문제를 해결하기 위해
 * 수동으로 Bean을 생성하여 dimensions를 명시적으로 설정합니다.
 */
@Slf4j
@Configuration
public class EmbeddingConfig {

	@Value("${spring.ai.google.genai.api-key:}")
	private String apiKey;

	@Value("${spring.ai.google.genai.embedding.text.options.model:gemini-embedding-001}")
	private String model;

	@Value("${spring.ai.google.genai.embedding.text.options.task-type:RETRIEVAL_DOCUMENT}")
	private String taskType;

	@Value("${spring.ai.google.genai.embedding.text.options.output-dimensionality:1536}")
	private Integer dimensions;

	/**
	 * Embedding Model Bean 생성
	 * dimensions를 명시적으로 설정하여 1536 차원을 보장합니다.
	 * 
	 * @param genAiClient Google GenAI Client (옵션, API 키를 직접 사용할 수도 있음)
	 * @return EmbeddingModel
	 */
	@Bean
	@Primary
	@ConditionalOnMissingBean(name = "embeddingModel")
	public EmbeddingModel embeddingModel(Client genAiClient) {
		log.info("=== Spring AI EmbeddingModel 초기화 (수동 생성) ===");
		log.info("사용 모델: {}", model);
		log.info("Task Type: {}", taskType);
		log.info("Dimensions: {} (명시적으로 설정됨)", dimensions);
		log.info("=====================================");

		// API 키를 사용하여 ConnectionDetails 생성
		String key = apiKey;
		if (key == null || key.isEmpty() || key.trim().isEmpty()) {
			key = System.getenv("GEMINI_API_KEY");
		}
		if (key == null || key.isEmpty()) {
			throw new IllegalStateException(
					"Google GenAI API key is not configured. Please set spring.ai.google.genai.api-key or GEMINI_API_KEY environment variable.");
		}

		GoogleGenAiEmbeddingConnectionDetails connectionDetails = GoogleGenAiEmbeddingConnectionDetails.builder()
				.apiKey(key)
				.build();

		GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
				.model(model)
				.taskType(TaskType.valueOf(taskType))
				.dimensions(dimensions) // 1536 차원을 명시적으로 설정
				.build();

		return new GoogleGenAiTextEmbeddingModel(connectionDetails, options);
	}
}
