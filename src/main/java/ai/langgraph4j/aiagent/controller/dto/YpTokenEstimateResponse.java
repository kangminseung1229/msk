package ai.langgraph4j.aiagent.controller.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 예규·판례(Yp) 임베딩 토큰 계산 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "예규판례 임베딩 토큰 계산 결과")
public class YpTokenEstimateResponse {

	@Schema(description = "대상 건수 (상위 N건)", example = "100")
	private Integer totalCount;

	@Schema(description = "사용 모델", example = "gemini-embedding-001")
	private String embeddingModel;

	@Schema(description = "모델당 1M 토큰 비용 (USD)", example = "0.15")
	private BigDecimal costPerMillionTokens;

	@Schema(description = "총 예상 청크 수", example = "120")
	private Integer totalChunks;

	@Schema(description = "총 예상 토큰 수", example = "180000")
	private Long totalTokens;

	@Schema(description = "예상 비용 (USD)", example = "0.027")
	private BigDecimal estimatedCost;

	@Schema(description = "건별 상세 정보")
	private List<YpTokenInfo> ypDetails;

	/**
	 * 예규판례 건별 토큰 정보
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(description = "예규판례 건별 토큰 정보")
	public static class YpTokenInfo {

		@Schema(description = "예규판례 ID", example = "1")
		private Long ypId;

		@Schema(description = "문선 번호", example = "2024-001")
		private String documentNumber;

		@Schema(description = "제목", example = "소득세 법인 과세요건")
		private String title;

		@Schema(description = "텍스트 길이 (문자 수)", example = "5000")
		private Integer textLength;

		@Schema(description = "예상 토큰 수", example = "1667")
		private Long estimatedTokens;

		@Schema(description = "예상 청크 수", example = "1")
		private Integer estimatedChunks;

		@Schema(description = "예상 비용 (USD)", example = "0.00025")
		private BigDecimal estimatedCost;
	}
}
