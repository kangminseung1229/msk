package ai.langgraph4j.msk.config;

import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Embedding Model 설정
 * Gemini Embedding을 사용하여 텍스트를 벡터로 변환합니다.
 * 
 * Spring AI 1.1.1의 auto-configuration을 사용합니다.
 * application.properties에서 다음 설정이 필요합니다:
 * - spring.ai.model.embedding.text=google-genai
 * - spring.ai.google.genai.embedding.api-key=${GEMINI_API_KEY}
 * 
 * Auto-configuration이 자동으로 GoogleGenAiTextEmbeddingModel Bean을 생성합니다.
 */
@Slf4j
@Configuration
public class EmbeddingConfig {
	// Spring AI auto-configuration을 사용하므로 별도의 Bean 정의가 필요하지 않습니다.
	// application.properties에서 설정하면 자동으로 EmbeddingModel Bean이 생성됩니다.
}
