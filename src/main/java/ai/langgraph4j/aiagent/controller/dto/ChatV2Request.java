package ai.langgraph4j.aiagent.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 v2 요청 DTO
 * LangGraph 기반 채팅 시스템의 요청
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatV2Request {

	/**
	 * 사용자 메시지 (필수)
	 */
	@NotBlank(message = "메시지는 필수입니다")
	private String message;

	/**
	 * 세션 ID (선택사항, 없으면 새 세션 생성)
	 */
	private String sessionId;

	/**
	 * System Instruction (선택사항)
	 */
	private String systemInstruction;

	/**
	 * 모델명 (선택사항, 기본값: gemini-2.5-flash)
	 */
	private String model;
}
