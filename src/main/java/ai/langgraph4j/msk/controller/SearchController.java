package ai.langgraph4j.msk.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.langgraph4j.msk.controller.dto.SearchRequest;
import ai.langgraph4j.msk.controller.dto.SearchResponse;
import ai.langgraph4j.msk.service.ConsultationSearchService;
import ai.langgraph4j.msk.service.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 벡터 검색 컨트롤러
 * 상담 데이터를 벡터 유사도 검색하는 API를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

	private final ConsultationSearchService searchService;

	/**
	 * 벡터 검색 (GET 요청)
	 * 
	 * @param query 검색 쿼리
	 * @param topK  반환할 최대 결과 수 (기본값: 5)
	 * @return 검색 결과
	 */
	@GetMapping
	public ResponseEntity<SearchResponse> search(
			@RequestParam String query,
			@RequestParam(required = false, defaultValue = "5") int topK) {
		log.info("검색 요청 (GET) - query: {}, topK: {}", query, topK);

		List<SearchResult> results = searchService.search(query, topK);

		return ResponseEntity.ok(SearchResponse.builder()
				.query(query)
				.topK(topK)
				.resultCount(results.size())
				.results(results)
				.build());
	}

	/**
	 * 벡터 검색 (POST 요청)
	 * 
	 * @param request 검색 요청
	 * @return 검색 결과
	 */
	@PostMapping
	public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
		log.info("검색 요청 (POST) - query: {}, topK: {}, threshold: {}",
				request.getQuery(), request.getTopK(), request.getSimilarityThreshold());

		List<SearchResult> results;

		if (request.getSimilarityThreshold() != null) {
			// 유사도 임계값이 지정된 경우
			results = searchService.search(
					request.getQuery(),
					request.getTopK() != null ? request.getTopK() : 5,
					request.getSimilarityThreshold());
		} else {
			// 기본 검색
			results = searchService.search(
					request.getQuery(),
					request.getTopK() != null ? request.getTopK() : 5);
		}

		return ResponseEntity.ok(SearchResponse.builder()
				.query(request.getQuery())
				.topK(request.getTopK() != null ? request.getTopK() : 5)
				.similarityThreshold(request.getSimilarityThreshold())
				.resultCount(results.size())
				.results(results)
				.build());
	}
}
