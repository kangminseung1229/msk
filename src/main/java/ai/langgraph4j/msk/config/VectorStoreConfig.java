package ai.langgraph4j.msk.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Vector Store 설정
 * PostgreSQL pgvector를 사용하여 벡터를 저장하고 검색합니다.
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

	/**
	 * Vector Store Bean 생성
	 * PostgreSQL pgvector를 사용합니다.
	 * 
	 * @param embeddingModel Embedding Model
	 * @param jdbcTemplate   JdbcTemplate (DataSource에서 자동 생성됨)
	 * @return VectorStore
	 */
	@Bean
	public VectorStore vectorStore(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
		log.info("VectorStore 생성: PgVectorStore (PostgreSQL pgvector)");

		VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
				.dimensions(768) // Gemini Embedding 차원
				.vectorTableName("spring_ai_vector_store") // 커스텀 테이블 이름
				.initializeSchema(false) // 테이블이 이미 존재하므로 자동 생성 비활성화
				.build();

		log.info("VectorStore 초기화 완료 (스키마는 수동으로 생성해야 합니다)");

		return vectorStore;
	}
}
