package ai.langgraph4j.msk.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 벡터 검색 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

	/**
	 * 상담 ID
	 */
	private Long consultationId;

	/**
	 * 상담 제목
	 */
	private String title;

	/**
	 * 검색된 청크 내용
	 */
	private String content;

	/**
	 * 상담 분야 대분류
	 */
	private String fieldLarge;

	/**
	 * 상담 생성일시
	 */
	private String createdAt;

	/**
	 * 청크 인덱스 (여러 청크로 나뉜 경우)
	 */
	private Integer chunkIndex;

	/**
	 * 전체 청크 수 (여러 청크로 나뉜 경우)
	 */
	private Integer totalChunks;

	/**
	 * 유사도 점수 (0.0 ~ 1.0, 높을수록 더 유사)
	 */
	private Double similarityScore;
}
