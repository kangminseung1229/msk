package ai.langgraph4j.msk.agent.nodes;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import ai.langgraph4j.msk.agent.state.AgentState;
import lombok.extern.slf4j.Slf4j;

/**
 * 도구 실행 노드
 * 
 * Phase 3: Spring AI Tool 자동 호출
 * Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달하므로,
 * 이 노드는 더 이상 필요하지 않을 수 있습니다.
 * 
 * 하지만 현재 구조를 유지하면서 Spring AI의 자동 Tool 호출을 활용하도록
 * 간소화된 버전으로 유지합니다.
 * 
 * 참고: Spring AI가 Tool을 자동으로 호출하므로, 이 노드는 사실상 빈 노드가 될 수 있습니다.
 */
@Slf4j
@Component
public class ToolNode {

	/**
	 * Phase 3: Spring AI Tool 자동 호출
	 * 
	 * Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달하므로,
	 * 이 노드는 더 이상 수동으로 Tool을 호출할 필요가 없습니다.
	 * 
	 * 현재 구조를 유지하면서 Spring AI의 자동 Tool 호출을 활용하도록
	 * 간소화된 버전으로 유지합니다.
	 * 
	 * @param state 현재 상태
	 * @return 업데이트된 상태
	 */
	public AgentState process(AgentState state) {
		log.debug("ToolNode: Phase 3 - Spring AI가 자동으로 Tool을 호출하므로 이 노드는 빈 노드입니다.");

		// Phase 3: Spring AI가 자동으로 Tool을 호출하므로, 이 노드는 더 이상 필요하지 않습니다.
		// 하지만 현재 구조를 유지하면서 Spring AI의 자동 Tool 호출을 활용하도록
		// 간소화된 버전으로 유지합니다.
		
		// 상태 업데이트
		state.setCurrentStep("tool");
		
		// 도구 실행 요청은 초기화 (Spring AI가 이미 처리했으므로)
		state.setToolExecutionRequests(new ArrayList<>());

		return state;
	}
}
