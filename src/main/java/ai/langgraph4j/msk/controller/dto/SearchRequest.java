package ai.langgraph4j.msk.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 검색 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

	/**
	 * 검색 쿼리
	 */
	private String query;

	/**
	 * 반환할 최대 결과 수 (기본값: 5)
	 */
	private Integer topK;

	/**
	 * 유사도 임계값 (0.0 ~ 1.0, 선택사항)
	 * 높을수록 더 유사한 결과만 반환
	 */
	private Double similarityThreshold;
}
