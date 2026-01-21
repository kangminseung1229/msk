package ai.langgraph4j.msk.service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.langgraph4j.msk.agent.graph.AgentGraph;
import ai.langgraph4j.msk.agent.nodes.ResponseNode;
import ai.langgraph4j.msk.agent.state.AgentState;
import ai.langgraph4j.msk.controller.dto.AgentRequest;
import ai.langgraph4j.msk.controller.dto.AgentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 에이전트 서비스
 * 에이전트 실행 로직을 처리하는 서비스 레이어입니다.
 * 
 * Phase 3: 스트리밍 지원 추가
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

	private final AgentGraph agentGraph;
	private final ResponseNode responseNode;

	/**
	 * 에이전트 실행
	 * 
	 * @param request 에이전트 실행 요청
	 * @return 에이전트 응답
	 */
	public AgentResponse invoke(AgentRequest request) {
		log.info("AgentService: 에이전트 실행 요청 - {}", request.getMessage());

		long startTime = System.currentTimeMillis();

		// 초기 상태 생성
		AgentState initialState = new AgentState();
		initialState.setSessionId(request.getSessionId());
		initialState.setSystemInstruction(request.getSystemInstruction());

		// 옵션 설정 (있는 경우)
		if (request.getOptions() != null) {
			// 옵션은 나중에 사용 가능
		}

		// 그래프 실행
		AgentState finalState = agentGraph.execute(initialState, request.getMessage());

		// 에러가 있는 경우
		if (finalState.getError() != null && !finalState.getError().isEmpty()) {
			throw new AgentExecutionException("AGENT_ERROR", finalState.getError());
		}

		// 응답 생성
		AgentResponse response = responseNode.process(finalState, startTime);

		log.info("AgentService: 에이전트 실행 완료 - 실행 시간: {}초", response.getExecutionTime());

		return response;
	}

	/**
	 * Phase 3: 스트리밍 모드로 에이전트 실행
	 * SSE(Server-Sent Events)를 사용하여 에이전트 실행 중간 결과를 실시간으로 전송합니다.
	 * 
	 * @param message           사용자 메시지
	 * @param sessionId         세션 ID
	 * @param systemInstruction System Instruction (선택사항)
	 * @return SseEmitter 스트리밍 응답을 위한 emitter
	 */
	public SseEmitter stream(String message, String sessionId, String systemInstruction) {
		log.info("AgentService: 스트리밍 요청 - message: {}, sessionId: {}, systemInstruction: {}", 
				message, sessionId, systemInstruction != null ? "있음" : "없음");

		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 타임아웃 없음

		CompletableFuture.runAsync(() -> {
			try {
				long startTime = System.currentTimeMillis();

				// 초기 상태 생성
				AgentState initialState = new AgentState();
				initialState.setSessionId(sessionId);
				initialState.setSystemInstruction(systemInstruction);

				// 스트리밍 시작 이벤트 전송
				emitter.send(SseEmitter.event()
						.name("start")
						.data("에이전트 실행을 시작합니다..."));

				// 그래프 실행 (스트리밍 모드)
				AgentState finalState = agentGraph.executeStreaming(initialState, message, emitter);

				// 에러가 있는 경우
				if (finalState.getError() != null && !finalState.getError().isEmpty()) {
					emitter.send(SseEmitter.event()
							.name("error")
							.data(finalState.getError()));
					emitter.complete();
					return;
				}

				// 최종 응답 생성
				AgentResponse response = responseNode.process(finalState, startTime);

				// 최종 응답 전송
				emitter.send(SseEmitter.event()
						.name("response")
						.data(response.getResponse()));

				// 완료 이벤트 전송
				emitter.send(SseEmitter.event()
						.name("complete")
						.data("에이전트 실행이 완료되었습니다. 실행 시간: " + response.getExecutionTime() + "초"));

				emitter.complete();

				log.info("AgentService: 스트리밍 완료 - 실행 시간: {}초", response.getExecutionTime());

			} catch (Exception e) {
				log.error("AgentService: 스트리밍 중 오류 발생", e);
				try {
					emitter.send(SseEmitter.event()
							.name("error")
							.data("에이전트 실행 중 오류가 발생했습니다: " + e.getMessage()));
					emitter.complete();
				} catch (IOException ioException) {
					log.error("AgentService: 에러 이벤트 전송 실패", ioException);
					emitter.completeWithError(ioException);
				}
			}
		});

		return emitter;
	}

	/**
	 * 에이전트 실행 예외
	 */
	public static class AgentExecutionException extends RuntimeException {
		private final String errorCode;

		public AgentExecutionException(String errorCode, String message) {
			super(message);
			this.errorCode = errorCode;
		}

		public String getErrorCode() {
			return errorCode;
		}
	}
}
