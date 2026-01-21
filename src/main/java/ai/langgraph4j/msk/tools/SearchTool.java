package ai.langgraph4j.msk.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * 검색 도구
 * AI 에이전트가 웹 검색을 수행할 수 있도록 하는 도구입니다.
 * 
 * Phase 2에서는 간단한 구조로 구현하며,
 * Phase 3에서 실제 검색 API (Google Search API, Bing Search API 등) 통합 예정입니다.
 * 
 * 지원 기능:
 * - 웹 검색 (검색어 기반)
 * - 검색 결과 요약
 * 
 * 참고: 현재는 구조만 구현되어 있으며, 실제 검색 API 통합은 Phase 3에서 진행합니다.
 */
@Slf4j
@Component
public class SearchTool {

	// Phase 3에서 실제 검색 API 통합 시 사용 예정
	@SuppressWarnings("unused")
	private final RestTemplate restTemplate;

	public SearchTool() {
		this.restTemplate = new RestTemplate();
	}

	/**
	 * 웹 검색을 수행합니다.
	 * 
	 * @param query 검색어
	 * @return 검색 결과 (문자열)
	 */
	@Tool(description = "웹 검색을 수행합니다. 검색어를 입력받아 관련 정보를 검색합니다.")
	public String search(
			@ToolParam(description = "검색할 키워드 또는 질문") String query) {
		if (query == null || query.trim().isEmpty()) {
			return "오류: 검색어가 비어있습니다.";
		}

		try {
			query = query.trim();
			log.debug("SearchTool: 검색 시작 - {}", query);

			// Phase 2: 간단한 응답 반환
			// Phase 3에서 실제 검색 API 통합 예정
			// 예: Google Custom Search API, Bing Search API 등
			
			// 현재는 검색어를 기반으로 간단한 응답 생성
			String result = performSearch(query);
			
			log.debug("SearchTool: 검색 완료 - {}", query);
			return result;

		} catch (RestClientException e) {
			log.error("SearchTool: 검색 API 호출 오류 - {}", query, e);
			return "오류: 검색 API 호출 중 오류가 발생했습니다. " + e.getMessage();
		} catch (Exception e) {
			log.error("SearchTool: 예상치 못한 오류 - {}", query, e);
			return "오류: 검색 중 예상치 못한 오류가 발생했습니다. " + e.getMessage();
		}
	}

	/**
	 * 실제 검색 수행
	 * 
	 * Phase 2: 간단한 응답 반환
	 * Phase 3: 실제 검색 API 통합
	 * 
	 * @param query 검색어
	 * @return 검색 결과
	 */
	private String performSearch(String query) {
		// Phase 2: 간단한 응답
		// 실제 검색 API가 없으므로 검색어를 기반으로 한 간단한 응답 반환
		// Phase 3에서 실제 API 통합 예정
		
		// 예시: Google Custom Search API 사용 시
		// String apiKey = "your-api-key";
		// String searchEngineId = "your-search-engine-id";
		// String url = String.format(
		//     "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s",
		//     apiKey, searchEngineId, URLEncoder.encode(query, StandardCharsets.UTF_8));
		// 
		// ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		// return parseSearchResults(response.getBody());
		
		// 현재는 검색어를 기반으로 한 안내 메시지 반환
		return String.format(
			"검색어 '%s'에 대한 검색 결과:\n" +
			"[Phase 2: 검색 기능은 구조만 구현되어 있습니다. Phase 3에서 실제 검색 API를 통합할 예정입니다.]\n" +
			"검색어: %s\n" +
			"실제 검색 API 통합 후 정확한 검색 결과를 제공할 수 있습니다.",
			query, query
		);
	}

	/**
	 * 검색 결과 요약
	 * 
	 * @param query 검색어
	 * @param maxResults 최대 결과 수
	 * @return 요약된 검색 결과
	 */
	public String searchSummary(String query, int maxResults) {
		if (query == null || query.trim().isEmpty()) {
			return "오류: 검색어가 비어있습니다.";
		}

		try {
			query = query.trim();
			log.debug("SearchTool: 검색 요약 시작 - {}, 최대 결과: {}", query, maxResults);

			// Phase 2: 간단한 응답
			String fullResult = search(query);
			
			// 요약 버전 반환 (실제로는 검색 결과를 요약)
			String summary = String.format(
				"검색어 '%s'에 대한 요약 (최대 %d개 결과):\n%s",
				query, maxResults, fullResult
			);
			
			log.debug("SearchTool: 검색 요약 완료 - {}", query);
			return summary;

		} catch (Exception e) {
			log.error("SearchTool: 검색 요약 오류 - {}", query, e);
			return "오류: 검색 요약 중 오류가 발생했습니다. " + e.getMessage();
		}
	}
}
