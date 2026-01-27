package ai.langgraph4j.aiagent.agent.nodes;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ai.langgraph4j.aiagent.agent.state.AgentState;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 답변 검수 노드
 * LLM이 생성한 답변을 검수하여 품질을 평가하고 필요시 재생성을 요청합니다.
 * 
 * v2: LangGraph 기반 채팅 시스템의 답변 검수 기능
 */
@Slf4j
@Component
public class ValidationNode {

	private final ChatModel chatModel;

	@Value("${agent.validation.enabled:true}")
	private boolean validationEnabled;

	@Value("${agent.validation.min-score:0.7}")
	private double minScore;

	public ValidationNode(@Qualifier("chatModel") ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * 답변 검수 수행
	 * 
	 * @param state 현재 상태
	 * @return 업데이트된 상태 (검수 결과 포함)
	 */
	public AgentState validate(AgentState state) {
		if (!validationEnabled) {
			log.debug("ValidationNode: 검수가 비활성화되어 있음");
			state.getMetadata().put("validationSkipped", true);
			return state;
		}

		if (state.getAiMessage() == null || state.getAiMessage().text() == null) {
			log.warn("ValidationNode: 검수할 AI 메시지가 없음");
			state.getMetadata().put("validationError", "AI 메시지가 없습니다");
			return state;
		}

		try {
			String aiResponse = state.getAiMessage().text();
			String userQuery = state.getUserMessage() != null ? state.getUserMessage().singleText() : "";

			// 검수 프롬프트 생성
			String validationPrompt = buildValidationPrompt(userQuery, aiResponse);

			// 검수 수행 (간단한 LLM 호출)
			String validationResult = performValidation(validationPrompt);

			// 검수 결과 파싱
			ValidationResult result = parseValidationResult(validationResult);

			// 검수 결과를 메타데이터에 저장
			state.getMetadata().put("validationScore", result.score);
			state.getMetadata().put("validationPassed", result.passed);
			state.getMetadata().put("validationFeedback", result.feedback);
			state.getMetadata().put("validationNeedsRegeneration", result.needsRegeneration);

			log.info("ValidationNode: 검수 완료 - 점수: {}, 통과: {}, 재생성 필요: {}",
					result.score, result.passed, result.needsRegeneration);

			// 검수 실패 시 에러 플래그 설정
			if (!result.passed && result.needsRegeneration) {
				log.warn("ValidationNode: 검수 실패 - 재생성 필요");
				state.getMetadata().put("validationFailed", true);
			}

			return state;

		} catch (Exception e) {
			log.error("ValidationNode: 검수 중 오류 발생", e);
			state.getMetadata().put("validationError", "검수 중 오류 발생: " + e.getMessage());
			// 검수 실패해도 계속 진행 (검수는 선택적 기능)
			return state;
		}
	}

	/**
	 * 검수 프롬프트 생성
	 */
	private String buildValidationPrompt(String userQuery, String aiResponse) {
		return String.format(
			"다음은 AI 어시스턴트가 사용자 질문에 대해 생성한 답변입니다. " +
			"답변의 품질을 평가하고 검수해주세요.\n\n" +
			"사용자 질문: %s\n\n" +
			"AI 답변: %s\n\n" +
			"다음 형식으로 JSON 응답을 제공해주세요:\n" +
			"{\n" +
			"  \"score\": 0.0-1.0 (답변 품질 점수),\n" +
			"  \"passed\": true/false (최소 점수 이상인지),\n" +
			"  \"feedback\": \"검수 피드백\",\n" +
			"  \"needsRegeneration\": true/false (재생성이 필요한지)\n" +
			"}\n\n" +
			"평가 기준:\n" +
			"- 정확성: 답변이 정확한 정보를 제공하는가?\n" +
			"- 관련성: 답변이 질문과 관련이 있는가?\n" +
			"- 완전성: 답변이 질문에 충분히 답하는가?\n" +
			"- 명확성: 답변이 명확하고 이해하기 쉬운가?\n" +
			"- 적절성: 답변이 적절한 톤과 스타일인가?",
			userQuery, aiResponse
		);
	}

	/**
	 * 검수 수행 (LLM 호출)
	 */
	private String performValidation(String prompt) {
		// 간단한 LLM 호출로 검수 수행
		// 실제로는 별도의 검수 전용 모델을 사용할 수도 있음
		// Spring AI ChatModel을 직접 사용
		org.springframework.ai.chat.messages.SystemMessage systemMsg = 
			new org.springframework.ai.chat.messages.SystemMessage(
				"당신은 AI 답변 검수 전문가입니다. 주어진 답변을 객관적으로 평가하고 JSON 형식으로 결과를 제공하세요.");
		org.springframework.ai.chat.messages.UserMessage userMsg = 
			new org.springframework.ai.chat.messages.UserMessage(prompt);

		org.springframework.ai.chat.prompt.Prompt springPrompt = 
			new org.springframework.ai.chat.prompt.Prompt(List.of(systemMsg, userMsg));
		
		org.springframework.ai.chat.model.ChatResponse response = chatModel.call(springPrompt);
		return response.getResult().getOutput().getText();
	}

	/**
	 * 검수 결과 파싱
	 */
	private ValidationResult parseValidationResult(String validationResult) {
		try {
			// JSON 파싱 시도
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			ValidationResult result = mapper.readValue(validationResult, ValidationResult.class);
			
			// 기본값 설정
			if (result.score < 0) result.score = 0.0;
			if (result.score > 1) result.score = 1.0;
			if (result.feedback == null) result.feedback = "검수 완료";
			
			// 최소 점수 체크
			result.passed = result.score >= minScore;
			
			// 재생성 필요 여부 결정 (점수가 낮거나 피드백에 재생성 언급이 있으면)
			if (!result.passed) {
				result.needsRegeneration = true;
			} else if (result.feedback != null && 
					(result.feedback.contains("재생성") || result.feedback.contains("수정") || result.feedback.contains("개선"))) {
				result.needsRegeneration = true;
			}
			
			return result;
		} catch (Exception e) {
			log.warn("ValidationNode: 검수 결과 파싱 실패, 기본값 사용 - {}", e.getMessage());
			// 파싱 실패 시 기본값 반환 (검수 통과로 간주)
			ValidationResult result = new ValidationResult();
			result.score = 0.8; // 기본 점수
			result.passed = true;
			result.feedback = "검수 결과 파싱 실패, 기본값 사용";
			result.needsRegeneration = false;
			return result;
		}
	}

	/**
	 * 검수 결과 내부 클래스
	 */
	@lombok.Data
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
	private static class ValidationResult {
		@com.fasterxml.jackson.annotation.JsonProperty("score")
		private double score = 0.0;
		
		@com.fasterxml.jackson.annotation.JsonProperty("passed")
		private boolean passed = false;
		
		@com.fasterxml.jackson.annotation.JsonProperty("feedback")
		private String feedback = "";
		
		@com.fasterxml.jackson.annotation.JsonProperty("needsRegeneration")
		private boolean needsRegeneration = false;
	}
}
