package ai.langgraph4j.aiagent.config;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 프롬프트 텍스트를 resources/prompts/에서 로드합니다.
 * 코드 내 하드코딩 대신 파일로 관리하여 수정·배포를 쉽게 합니다.
 */
@Slf4j
@Component
public class PromptConfig {

	private static final String SEARCH_QUERY_CONTEXT_PATH = "prompts/search-query-context.txt";
	private static final String DEFAULT_SEARCH_QUERY_CONTEXT =
			"당신은 세무 회계 상담가 입니다. 질문 파악, 핵심 쟁점, 결론, 근거를 제시하며 답변하세요.";

	private final String searchQueryContextPrefix;

	public PromptConfig() {
		this.searchQueryContextPrefix = loadSearchQueryContextPrefix();
	}

	/**
	 * hybridSearch용 검색 쿼리 보강 문구 (systemInstruction과 함께 사용)
	 */
	public String getSearchQueryContextPrefix() {
		return searchQueryContextPrefix;
	}

	private String loadSearchQueryContextPrefix() {
		try {
			var resource = new ClassPathResource(SEARCH_QUERY_CONTEXT_PATH);
			if (!resource.exists()) {
				log.warn("프롬프트 파일 없음: {}, 기본값 사용", SEARCH_QUERY_CONTEXT_PATH);
				return DEFAULT_SEARCH_QUERY_CONTEXT;
			}
			String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
			String trimmed = content != null ? content.trim() : "";
			if (trimmed.isEmpty()) {
				log.warn("프롬프트 파일이 비어 있음: {}, 기본값 사용", SEARCH_QUERY_CONTEXT_PATH);
				return DEFAULT_SEARCH_QUERY_CONTEXT;
			}
			log.info("프롬프트 로드 완료: {} ({}자)", SEARCH_QUERY_CONTEXT_PATH, trimmed.length());
			return trimmed;
		} catch (Exception e) {
			log.warn("프롬프트 로드 실패: {}, 기본값 사용. {}", SEARCH_QUERY_CONTEXT_PATH, e.getMessage());
			return DEFAULT_SEARCH_QUERY_CONTEXT;
		}
	}
}
