package ai.langgraph4j.msk.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 텍스트 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextGenerationRequest {
	
	/**
	 * 사용자 입력 프롬프트
	 */
	private String prompt;
	
	/**
	 * 사용할 모델명 (선택사항, 기본값: gemini-3-flash-preview)
	 */
	private String model;
	
	/**
	 * thoughts 포함 여부 (선택사항, 기본값: false)
	 * true로 설정하면 응답에 grounding_metadata나 thought 필드가 포함될 수 있습니다.
	 */
	private Boolean includeThoughts;
}
