package ai.langgraph4j.msk.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 에이전트 실행 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {

	/**
	 * 사용자 메시지 (필수)
	 */
	@NotBlank(message = "메시지는 필수입니다.")
	private String message;

	/**
	 * 세션 ID (선택사항, 대화 히스토리 유지용)
	 */
	private String sessionId;

	/**
	 * System Instruction (선택사항, 모델의 역할과 동작 방식을 정의)
	 */
	private String systemInstruction;

	/**
	 * 추가 옵션
	 */
	private AgentOptions options;
}
