package ai.langgraph4j.msk.agent.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 에이전트의 상태를 정의하는 클래스
 * LangGraph4j의 StateGraph에서 사용할 상태 스키마입니다.
 * 
 * Phase 1 설계에 따라 확장된 상태 구조입니다.
 * 
 * 참고: LangGraph4j의 AgentState를 상속하지 않고 독립적인 상태 클래스로 구현합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AgentState {

	/**
	 * 사용자 메시지
	 */
	private UserMessage userMessage;

	/**
	 * AI 응답 메시지
	 */
	private AiMessage aiMessage;

	/**
	 * 도구 실행 요청 목록
	 */
	private List<ToolExecutionRequest> toolExecutionRequests = new ArrayList<>();

	/**
	 * 도구 실행 결과 목록
	 */
	private List<ToolExecutionResult> toolExecutionResults = new ArrayList<>();

	/**
	 * 대화 히스토리 (전체 메시지)
	 */
	private List<Object> messages = new ArrayList<>();

	/**
	 * 현재 단계 추적
	 * 가능한 값: "input", "llm", "tool", "response", "error"
	 */
	private String currentStep;

	/**
	 * 반복 횟수 (무한 루프 방지)
	 */
	private int iterationCount = 0;

	/**
	 * 세션 ID (대화 히스토리 유지용)
	 */
	private String sessionId;

	/**
	 * 에러 메시지 (있는 경우)
	 */
	private String error;

	/**
	 * 예외 정보 (있는 경우)
	 */
	private Exception exception;

	/**
	 * 메타데이터 (추가 정보 저장용)
	 */
	private Map<String, Object> metadata = new HashMap<>();

	/**
	 * System Instruction (모델의 역할과 동작 방식을 정의)
	 */
	private String systemInstruction;

	/**
	 * 반복 횟수 증가
	 */
	public void incrementIterationCount() {
		this.iterationCount++;
	}

	/**
	 * 도구 실행 결과를 나타내는 내부 클래스
	 * (실제 구현 시 LangChain4j의 ToolExecutionResult 사용 가능)
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ToolExecutionResult {
		private String toolName;
		private String result;
		private boolean success;
		private String error;

		public ToolExecutionResult(String toolName, String result, boolean success) {
			this.toolName = toolName;
			this.result = result;
			this.success = success;
		}
	}
}
