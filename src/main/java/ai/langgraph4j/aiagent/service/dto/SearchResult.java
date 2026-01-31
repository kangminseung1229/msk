package ai.langgraph4j.aiagent.service.dto;

import java.util.List;

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
	private Long counselId;

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

	/**
	 * 문서 타입 ("counsel", "lawArticle", "yp")
	 */
	private String documentType;

	/**
	 * 예규·판례 ID (documentType == "yp"일 때)
	 */
	private Long ypId;

	/**
	 * 연관 법령 조문 정보 리스트
	 */
	private List<LawArticleInfo> lawArticles;

	/**
	 * 법령 조문 정보 DTO
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class LawArticleInfo {
		/**
		 * 법령 ID
		 */
		private String lawId;

		/**
		 * 법령명 (한국어)
		 */
		private String lawNameKorean;

		/**
		 * 조문 키
		 */
		private String articleKey;

		/**
		 * 조문 한국어 표현 (예: "제 10조의 21")
		 */
		private String articleKoreanString;

		/**
		 * 조문제목
		 */
		private String articleTitle;

		/**
		 * 조문 내용 (articleLinkContent 또는 articleOriginalContent)
		 */
		private String articleContent;
	}
}
