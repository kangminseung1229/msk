package ai.langgraph4j.msk.controller.dto;

import java.util.List;

import ai.langgraph4j.msk.service.dto.SearchResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 검색 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

	/**
	 * 검색 쿼리
	 */
	private String query;

	/**
	 * 요청한 최대 결과 수
	 */
	private Integer topK;

	/**
	 * 사용된 유사도 임계값 (있는 경우)
	 */
	private Double similarityThreshold;

	/**
	 * 실제 반환된 결과 수
	 */
	private Integer resultCount;

	/**
	 * 검색 결과 리스트
	 */
	private List<SearchResult> results;
}
