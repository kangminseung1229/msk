package ai.langgraph4j.msk.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * System Instruction 요청 DTO
 * System Instruction은 모델의 역할, 동작 방식, 응답 스타일 등을 정의합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemInstructionRequest {
	
	/**
	 * System Instruction: 모델의 역할과 동작 방식을 정의하는 지시사항
	 * 예시:
	 * - "당신은 친절한 AI 어시스턴트입니다. 항상 존댓말로 답변하세요."
	 * - "당신은 의학 전문가입니다. 정확하고 신중하게 답변하세요."
	 * - "JSON 형식으로만 답변하세요."
	 */
	private String systemInstruction;
	
	/**
	 * User Prompt: 실제 사용자의 질문이나 요청
	 * 예시:
	 * - "인공지능이 무엇인지 설명해주세요"
	 * - "감기 증상과 치료법을 알려주세요"
	 */
	private String userPrompt;
	
	/**
	 * 사용할 모델명 (선택사항, 기본값: gemini-3-flash-preview)
	 */
	private String model;
}
