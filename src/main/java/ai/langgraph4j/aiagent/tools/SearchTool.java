package ai.langgraph4j.aiagent.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ai.langgraph4j.aiagent.service.ConsultationSearchService;
import ai.langgraph4j.aiagent.service.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 검색 도구
 * AI 에이전트가 상담 데이터를 벡터 검색할 수 있도록 하는 도구입니다.
 * 
 * RAG (Retrieval-Augmented Generation) 패턴을 사용하여:
 * 1. 사용자 질문에 대한 관련 상담 데이터를 검색
 * 2. 검색 결과를 LLM에 컨텍스트로 제공하여 정확한 답변 생성
 * 
 * 지원 기능:
 * - 벡터 유사도 검색 (상담 데이터)
 * - 검색 결과 포맷팅 (LLM에 전달 가능한 형태)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchTool {

	private final ConsultationSearchService consultationSearchService;

	// Phase 3에서 실제 웹 검색 API 통합 시 사용 예정
	@SuppressWarnings("unused")
	private final RestTemplate restTemplate = new RestTemplate();

	/**
	 * 상담 데이터를 벡터 검색합니다.
	 * RAG 패턴을 사용하여 관련 상담 데이터를 검색하고 LLM에 컨텍스트로 제공합니다.
	 * 
	 * @param query 검색어 또는 질문
	 * @return 검색 결과 (포맷팅된 문자열)
	 */
	@Tool(description = "상담 데이터를 벡터 검색합니다. 사용자의 질문과 관련된 과거 상담 사례를 검색하여 답변에 참고할 수 있습니다.")
	public String search(
			@ToolParam(description = "검색할 키워드 또는 질문") String query) {
		if (query == null || query.trim().isEmpty()) {
			return "오류: 검색어가 비어있습니다.";
		}

		try {
			query = query.trim();
			log.info("SearchTool: 하이브리드 검색 시작 - {}", query);

			// 하이브리드 검색 수행 (상담 10건 + 법령 10건 + 연관 법령)
			List<SearchResult> results = consultationSearchService.hybridSearch(query, 10, 10, 0.6);

			if (results.isEmpty()) {
				log.warn("SearchTool: 검색 결과가 없습니다 - {}", query);
				return String.format(
						"검색어 '%s'에 대한 관련 상담 사례를 찾을 수 없습니다.\n" +
								"다른 키워드로 검색하거나 질문을 더 구체적으로 작성해주세요.",
						query);
			}

			// 검색 결과를 LLM에 전달 가능한 형태로 포맷팅
			String formattedResult = formatSearchResults(query, results);

			log.info("SearchTool: 하이브리드 검색 완료 - {}건의 결과 반환", results.size());
			return formattedResult;

		} catch (Exception e) {
			log.error("SearchTool: 검색 중 오류 발생 - {}", query, e);
			return "오류: 검색 중 오류가 발생했습니다. " + e.getMessage();
		}
	}

	/**
	 * 검색 결과를 LLM에 전달 가능한 형태로 포맷팅
	 * 
	 * @param query   검색 쿼리
	 * @param results 검색 결과 리스트
	 * @return 포맷팅된 검색 결과 문자열
	 */
	private String formatSearchResults(String query, List<SearchResult> results) {
		StringBuilder sb = new StringBuilder();
		sb.append("검색어: ").append(query).append("\n");
		sb.append("검색 결과 (").append(results.size()).append("건):\n\n");

		// 상담 결과와 법령 결과를 구분하여 표시
		List<SearchResult> counselResults = results.stream()
				.filter(r -> "counsel".equals(r.getDocumentType()))
				.toList();
		List<SearchResult> lawArticleResults = results.stream()
				.filter(r -> "lawArticle".equals(r.getDocumentType()))
				.toList();

		if (!counselResults.isEmpty()) {
			sb.append("=== 상담 사례 (").append(counselResults.size()).append("건) ===\n\n");
			for (int i = 0; i < counselResults.size(); i++) {
				SearchResult result = counselResults.get(i);
				formatSearchResult(sb, i + 1, result, true);
			}
		}

		if (!lawArticleResults.isEmpty()) {
			sb.append("\n=== 법령 조문 (").append(lawArticleResults.size()).append("건) ===\n\n");
			for (int i = 0; i < lawArticleResults.size(); i++) {
				SearchResult result = lawArticleResults.get(i);
				formatSearchResult(sb, i + 1, result, false);
			}
		}

		sb.append("\n위 검색 결과를 참고하여 사용자의 질문에 답변해주세요.");

		return sb.toString();
	}

	/**
	 * 개별 검색 결과를 포맷팅
	 * 
	 * @param sb        StringBuilder
	 * @param index     결과 인덱스
	 * @param result    검색 결과
	 * @param isCounsel 상담 결과 여부
	 */
	private void formatSearchResult(StringBuilder sb, int index, SearchResult result, boolean isCounsel) {
		sb.append("[").append(index).append("] ");

		if (result.getTitle() != null) {
			sb.append("제목: ").append(result.getTitle()).append("\n");
		}

		if (isCounsel && result.getFieldLarge() != null) {
			sb.append("     분야: ").append(result.getFieldLarge()).append("\n");
		}

		if (result.getContent() != null) {
			// 내용이 너무 길면 잘라서 표시
			String content = result.getContent();
			if (content.length() > 500) {
				content = content.substring(0, 500) + "...";
			}
			sb.append("     내용: ").append(content).append("\n");
		}

		// 연관 법령 조문 정보 추가 (상담 결과인 경우)
		if (isCounsel && result.getLawArticles() != null && !result.getLawArticles().isEmpty()) {
			sb.append("     연관 법령 조문:\n");
			for (SearchResult.LawArticleInfo lawArticle : result.getLawArticles()) {
				sb.append("       - ");
				if (lawArticle.getLawNameKorean() != null) {
					sb.append(lawArticle.getLawNameKorean()).append(" ");
				}
				if (lawArticle.getArticleKoreanString() != null) {
					sb.append(lawArticle.getArticleKoreanString());
				} else if (lawArticle.getArticleKey() != null) {
					sb.append("(조문키: ").append(lawArticle.getArticleKey()).append(")");
				}
				if (lawArticle.getArticleTitle() != null) {
					sb.append(" - ").append(lawArticle.getArticleTitle());
				}
				sb.append("\n");

				// 조문 내용 추가 (너무 길면 잘라서 표시)
				if (lawArticle.getArticleContent() != null) {
					String articleContent = lawArticle.getArticleContent();
					if (articleContent.length() > 300) {
						articleContent = articleContent.substring(0, 300) + "...";
					}
					sb.append("         조문 내용: ").append(articleContent).append("\n");
				}
			}
		}

		if (result.getSimilarityScore() != null) {
			sb.append("     유사도: ").append(String.format("%.2f", result.getSimilarityScore())).append("\n");
		}

		if (isCounsel && result.getCounselId() != null) {
			sb.append("     상담ID: ").append(result.getCounselId()).append("\n");
		}

		sb.append("\n");
	}

	/**
	 * 상담 데이터를 벡터 검색합니다 (결과 수 지정 가능)
	 * 
	 * @param query 검색어 또는 질문
	 * @param topK  반환할 최대 결과 수 (기본값: 5)
	 * @return 검색 결과 (포맷팅된 문자열)
	 */
	@Tool(description = "상담 데이터를 벡터 검색합니다. 결과 수를 지정할 수 있습니다.")
	public String searchWithLimit(
			@ToolParam(description = "검색할 키워드 또는 질문") String query,
			@ToolParam(description = "반환할 최대 결과 수", required = false) Integer topK) {
		if (query == null || query.trim().isEmpty()) {
			return "오류: 검색어가 비어있습니다.";
		}

		try {
			query = query.trim();
			int limit = (topK != null && topK > 0) ? topK : 5;
			log.info("SearchTool: 벡터 검색 시작 - {}, topK: {}", query, limit);

			// 벡터 검색 수행
			List<SearchResult> results = consultationSearchService.search(query, limit, 0.6);

			if (results.isEmpty()) {
				log.warn("SearchTool: 검색 결과가 없습니다 - {}", query);
				return String.format(
						"검색어 '%s'에 대한 관련 상담 사례를 찾을 수 없습니다.",
						query);
			}

			// 검색 결과 포맷팅
			String formattedResult = formatSearchResults(query, results);

			log.info("SearchTool: 벡터 검색 완료 - {}건의 결과 반환", results.size());
			return formattedResult;

		} catch (Exception e) {
			log.error("SearchTool: 검색 중 오류 발생 - {}", query, e);
			return "오류: 검색 중 오류가 발생했습니다. " + e.getMessage();
		}
	}
}
