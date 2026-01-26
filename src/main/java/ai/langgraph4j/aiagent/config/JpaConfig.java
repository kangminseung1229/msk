package ai.langgraph4j.aiagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

/**
 * JPA 설정
 * QueryDSL을 위한 JPAQueryFactory 빈을 제공합니다.
 * test 프로파일에서는 JPA/DataSource가 제외되므로 로드하지 않습니다.
 */
@Configuration
@org.springframework.context.annotation.Profile("!test")
public class JpaConfig {

	/**
	 * JPAQueryFactory 빈 생성
	 * QueryDSL 쿼리를 작성하기 위해 사용됩니다.
	 * 
	 * @param entityManager EntityManager
	 * @return JPAQueryFactory
	 */
	@Bean
	public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
		return new JPAQueryFactory(entityManager);
	}
}