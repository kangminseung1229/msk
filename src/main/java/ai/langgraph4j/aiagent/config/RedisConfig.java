package ai.langgraph4j.aiagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Redis 설정
 * v2: LangGraph 기반 채팅 시스템의 세션 관리를 위한 Redis 설정
 */
@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		
		// Key는 String으로 직렬화
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		
		// Value는 String으로 직렬화 (JSON 문자열로 저장)
		template.setValueSerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new StringRedisSerializer());
		
		template.afterPropertiesSet();
		return template;
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		// activateDefaultTyping은 deprecated되었고, 
		// 대신 PolymorphicTypeValidator를 사용해야 하지만,
		// 여기서는 단순히 JSON 문자열로 저장하므로 필요 없음
		// mapper.activateDefaultTyping() 제거
		return mapper;
	}
}
