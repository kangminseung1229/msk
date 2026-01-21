package ai.langgraph4j.msk.agent.nodes;

import ai.langgraph4j.msk.agent.state.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 조건 분기 노드
 * 현재 상태를 기반으로 다음 노드를 결정합니다.
 * 
 * Phase 3: Spring AI Tool 자동 호출
 * Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달하므로,
 * Tool 실행 요청 체크가 필요 없습니다. Spring AI가 내부에서 모든 Tool 실행을 처리합니다.
 */
@Slf4j
@Component
public class ConditionalNode {

	@Value("${agent.max-iterations:5}")
	private int maxIterations;

	/**
	 * 다음 노드를 결정하는 라우팅 함수
	 * 
	 * Phase 3: Spring AI Tool 자동 호출
	 * Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달하므로,
	 * Tool 실행 요청 체크가 필요 없습니다. Spring AI가 내부에서 모든 Tool 실행을 처리합니다.
	 * 
	 * @param state 현재 상태
	 * @return 다음 노드 이름 ("response", "error")
	 */
	public String route(AgentState state) {
		log.debug("ConditionalNode: 다음 노드 결정 시작, 반복 횟수: {}", state.getIterationCount());
		
		// 최대 반복 횟수 초과 체크
		if (state.getIterationCount() > maxIterations) {
			log.warn("ConditionalNode: 최대 반복 횟수 초과 ({}), 에러 노드로 이동", maxIterations);
			state.setError("최대 반복 횟수를 초과했습니다. 요청이 너무 복잡합니다.");
			state.setCurrentStep("error");
			return "error";
		}
		
		// Phase 3: Spring AI Tool 자동 호출
		// Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달하므로,
		// Tool 실행 요청 체크가 필요 없습니다. Spring AI가 내부에서 모든 Tool 실행을 처리합니다.
		// 따라서 Tool 실행 요청 체크 로직을 제거하고, 바로 응답 완료 또는 에러만 체크합니다.
		
		// 에러가 있는 경우
		if (state.getError() != null && !state.getError().isEmpty()) {
			log.debug("ConditionalNode: 에러 발생, ErrorNode로 이동");
			return "error";
		}
		
		// 정상 응답 완료
		// Spring AI가 Tool을 자동으로 호출하고 최종 응답을 생성했으므로, 바로 응답 완료로 처리합니다.
		log.debug("ConditionalNode: 응답 완료, ResponseNode로 이동");
		return "response";
	}
}
