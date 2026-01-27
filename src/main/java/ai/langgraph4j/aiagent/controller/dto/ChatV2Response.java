package ai.langgraph4j.aiagent.controller.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 v2 응답 DTO
 * LangGraph 기반 채팅 시스템의 응답
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatV2Response {

	/**
	 * AI 응답 메시지
	 */
	private String response;

	/**
	 * 세션 ID
	 */
	private String sessionId;

	/**
	 * 검수 결과 (검수 활성화 시)
	 */
	private ValidationResult validation;

	/**
	 * 실행 시간 (초)
	 */
	private Double executionTime;

	/**
	 * 메타데이터
	 */
	private Map<String, Object> metadata;

	/**
	 * 검수 결과 내부 클래스
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ValidationResult {
		private Double score;
		private Boolean passed;
		private String feedback;
		private Boolean needsRegeneration;
	}
}
