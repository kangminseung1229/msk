package ai.langgraph4j.aiagent.agent.graph;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.langgraph4j.aiagent.agent.nodes.ConditionalNode;
import ai.langgraph4j.aiagent.agent.nodes.InputNode;
import ai.langgraph4j.aiagent.agent.nodes.LlmNode;
import ai.langgraph4j.aiagent.agent.nodes.ResponseNode;
import ai.langgraph4j.aiagent.agent.nodes.ToolNode;
import ai.langgraph4j.aiagent.agent.state.AgentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 에이전트 그래프 정의
 * LangGraph4j StateGraph를 사용하여 에이전트 워크플로우를 구성합니다.
 * 
 * Phase 3: Spring AI Tool 자동 호출
 * Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달하므로,
 * 워크플로우가 단순화되었습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentGraph {

	private final InputNode inputNode;
	private final LlmNode llmNode;
	private final ConditionalNode conditionalNode;
	private final ResponseNode responseNode;
	// Phase 3: ToolNode는 더 이상 필요하지 않지만, 호환성을 위해 유지
	@SuppressWarnings("unused")
	private final ToolNode toolNode;

	/**
	 * 그래프 실행
	 * 
	 * Phase 3: Spring AI Tool 자동 호출
	 * Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달하므로,
	 * 워크플로우가 단순화되었습니다.
	 * 
	 * 그래프 흐름:
	 * 1. InputNode -> 사용자 입력 처리
	 * 2. LlmNode -> LLM 호출 (Spring AI가 Tool을 자동으로 호출하고 결과를 LLM에 전달)
	 * 3. ConditionalNode -> 다음 단계 결정
	 *    - "response" -> 종료
	 *    - "error" -> 종료
	 * 
	 * 참고: Spring AI가 내부에서 Tool 실행을 처리하므로, ToolNode 단계가 필요 없습니다.
	 * 
	 * @param initialState 초기 상태
	 * @param userInput 사용자 입력
	 * @return 최종 상태
	 */
	public AgentState execute(AgentState initialState, String userInput) {
		log.info("AgentGraph: 그래프 실행 시작 - 입력: {}", userInput);
		
		AgentState state = initialState;
		
		// 1. InputNode: 사용자 입력 처리
		state = inputNode.process(state, userInput);
		if (state.getError() != null) {
			log.warn("AgentGraph: InputNode에서 에러 발생 - {}", state.getError());
			return state;
		}
		
		// Phase 3: Spring AI Tool 자동 호출
		// Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달하므로,
		// 단순한 워크플로우로 충분합니다.
		// 최대 반복 횟수는 LlmNode와 ConditionalNode에서 체크합니다.
		
		// 2. LlmNode: LLM 호출 (Spring AI가 Tool을 자동으로 호출하고 결과를 LLM에 전달)
		state = llmNode.process(state);
		if (state.getError() != null) {
			log.warn("AgentGraph: LlmNode에서 에러 발생 - {}", state.getError());
			return state;
		}
		
		// 3. ConditionalNode: 다음 단계 결정
		String nextStep = conditionalNode.route(state);
		log.debug("AgentGraph: 다음 단계 결정 - {}", nextStep);
		
		// 4. 조건부 분기 처리
		if ("response".equals(nextStep)) {
			// 응답 완료 - Controller에서 ResponseNode로 처리
			log.info("AgentGraph: 응답 완료");
			return state;
		}
		
		if ("error".equals(nextStep)) {
			// 에러 발생
			log.warn("AgentGraph: 에러 발생 - {}", state.getError());
			return state;
		}
		
		// 예상치 못한 nextStep (Phase 3에서는 "tool"이 반환되지 않아야 함)
		log.warn("AgentGraph: 예상치 못한 nextStep - {}", nextStep);
		state.setError("예상치 못한 그래프 상태: " + nextStep);
		return state;
	}

	/**
	 * Phase 3: 스트리밍 모드로 그래프 실행
	 * SSE를 통해 중간 결과를 실시간으로 전송합니다.
	 * 
	 * @param initialState 초기 상태
	 * @param userInput 사용자 입력
	 * @param emitter SSE emitter (중간 결과 전송용)
	 * @return 최종 상태
	 */
	public AgentState executeStreaming(AgentState initialState, String userInput, SseEmitter emitter) {
		log.info("AgentGraph: 스트리밍 모드로 그래프 실행 시작 - 입력: {}", userInput);
		
		AgentState state = initialState;
		
		try {
			// 1. InputNode: 사용자 입력 처리
			emitter.send(SseEmitter.event()
					.name("step")
					.data("입력 처리 중..."));
			state = inputNode.process(state, userInput);
			if (state.getError() != null) {
				log.warn("AgentGraph: InputNode에서 에러 발생 - {}", state.getError());
				emitter.send(SseEmitter.event()
						.name("error")
						.data("입력 처리 중 오류: " + state.getError()));
				return state;
			}

			// 2. LlmNode: LLM 호출 (스트리밍 모드)
			emitter.send(SseEmitter.event()
					.name("step")
					.data("LLM 응답 생성 중..."));
			state = llmNode.processStreaming(state, emitter);
			if (state.getError() != null) {
				log.warn("AgentGraph: LlmNode에서 에러 발생 - {}", state.getError());
				emitter.send(SseEmitter.event()
						.name("error")
						.data("LLM 호출 중 오류: " + state.getError()));
				return state;
			}

			// 3. ConditionalNode: 다음 단계 결정
			emitter.send(SseEmitter.event()
					.name("step")
					.data("다음 단계 결정 중..."));
			String nextStep = conditionalNode.route(state);
			log.debug("AgentGraph: 다음 단계 결정 - {}", nextStep);

			// 4. 조건부 분기 처리
			if ("response".equals(nextStep)) {
				// 응답 완료 - 스트리밍 완료를 명확히 알리는 이벤트 전송
				log.info("AgentGraph: 응답 완료");
				emitter.send(SseEmitter.event()
						.name("streaming-complete")
						.data("스트리밍 완료"));
				return state;
			}
			
			if ("error".equals(nextStep)) {
				// 에러 발생
				log.warn("AgentGraph: 에러 발생 - {}", state.getError());
				emitter.send(SseEmitter.event()
						.name("error")
						.data("에러 발생: " + state.getError()));
				return state;
			}
			
			// 예상치 못한 nextStep
			log.warn("AgentGraph: 예상치 못한 nextStep - {}", nextStep);
			state.setError("예상치 못한 그래프 상태: " + nextStep);
			emitter.send(SseEmitter.event()
					.name("error")
					.data("예상치 못한 그래프 상태: " + nextStep));
			return state;
			
		} catch (IOException e) {
			log.error("AgentGraph: 스트리밍 중 IO 오류 발생", e);
			state.setError("스트리밍 중 오류 발생: " + e.getMessage());
			try {
				emitter.send(SseEmitter.event()
						.name("error")
						.data("스트리밍 중 오류: " + e.getMessage()));
			} catch (IOException ioException) {
				log.error("AgentGraph: 에러 이벤트 전송 실패", ioException);
			}
			return state;
		}
	}
}
