package ai.langgraph4j.aiagent.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.langgraph4j.aiagent.agent.graph.AgentGraph;
import ai.langgraph4j.aiagent.agent.nodes.ResponseNode;
import ai.langgraph4j.aiagent.agent.nodes.ValidationNode;
import ai.langgraph4j.aiagent.agent.state.AgentState;
import ai.langgraph4j.aiagent.controller.dto.ChatV2Request;
import ai.langgraph4j.aiagent.controller.dto.ChatV2Response;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 v2 서비스
 * LangGraph 기반 채팅 시스템의 비즈니스 로직
 * 
 * 주요 기능:
 * - 세션 기반 대화 컨텍스트 유지 (Redis)
 * - LangGraph를 활용한 에이전트 실행
 * - 답변 검수 기능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatV2Service {

	private final AgentGraph agentGraph;
	private final ResponseNode responseNode;
	private final ValidationNode validationNode;
	private final SessionStore sessionStore;
	private final ResourceLoader resourceLoader;

	/**
	 * 채팅 실행 (비스트리밍)
	 * 
	 * @param request 채팅 요청
	 * @return 채팅 응답
	 */
	public ChatV2Response chat(ChatV2Request request) {
		long startTime = System.currentTimeMillis();
		log.info("ChatV2Service: 채팅 요청 - message: {}, sessionId: {}", 
				request.getMessage(), request.getSessionId());

		// 세션 ID 생성 또는 사용
		String sessionId = request.getSessionId();
		if (sessionId == null || sessionId.isBlank()) {
			sessionId = generateSessionId();
			log.info("ChatV2Service: 새 세션 생성 - sessionId: {}", sessionId);
		}

		// System Instruction 처리 (비어있으면 기본값 사용)
		String systemInstruction = getSystemInstruction(request.getSystemInstruction());
		
		// 기존 세션 로드 (대화 히스토리 포함)
		AgentState initialState = loadOrCreateSession(sessionId, systemInstruction);

		// 사용자 메시지 추가
		UserMessage userMessage = new UserMessage(request.getMessage());
		initialState.setUserMessage(userMessage);

		// LangGraph 실행
		AgentState finalState = agentGraph.execute(initialState, request.getMessage());

		// 에러 처리
		if (finalState.getError() != null && !finalState.getError().isEmpty()) {
			throw new ChatV2Exception("CHAT_ERROR", finalState.getError());
		}

		// 답변 검수
		finalState = validationNode.validate(finalState);

		// 대화 히스토리 저장
		saveToHistory(sessionId, userMessage, finalState.getAiMessage());

		// 세션 저장
		sessionStore.saveSession(sessionId, finalState);

		// 응답 생성
		ChatV2Response response = buildResponse(finalState, sessionId, startTime);

		log.info("ChatV2Service: 채팅 완료 - sessionId: {}, 실행 시간: {}초", 
				sessionId, response.getExecutionTime());

		return response;
	}

	/**
	 * 채팅 실행 (스트리밍)
	 * 
	 * @param request 채팅 요청
	 * @return SseEmitter
	 */
	public SseEmitter chatStreaming(ChatV2Request request) {
		log.info("ChatV2Service: 스트리밍 채팅 요청 - message: {}, sessionId: {}", 
				request.getMessage(), request.getSessionId());

		// 세션 ID 생성 또는 사용
		String sessionId = request.getSessionId();
		if (sessionId == null || sessionId.isBlank()) {
			sessionId = generateSessionId();
			log.info("ChatV2Service: 새 세션 생성 - sessionId: {}", sessionId);
		}

		// System Instruction 처리 (비어있으면 기본값 사용)
		String systemInstruction = getSystemInstruction(request.getSystemInstruction());
		
		// 람다 표현식에서 사용할 final 변수들
		final String finalSessionId = sessionId;
		final String message = request.getMessage();
		final String finalSystemInstruction = systemInstruction;
		final String originalSessionId = request.getSessionId();

		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

		java.util.concurrent.CompletableFuture.runAsync(() -> {
			try {
				long startTime = System.currentTimeMillis();

				// 기존 세션 로드
				AgentState initialState = loadOrCreateSession(finalSessionId, finalSystemInstruction);

				// 사용자 메시지 추가
				UserMessage userMessage = new UserMessage(message);
				initialState.setUserMessage(userMessage);

				// 스트리밍 시작 이벤트 (세션 ID 포함)
				emitter.send(SseEmitter.event()
						.name("start")
						.data("채팅을 시작합니다..."));
				
				// 세션 ID 전송 (새 세션이 생성된 경우)
				if (originalSessionId == null || originalSessionId.isBlank()) {
					emitter.send(SseEmitter.event()
							.name("session")
							.data(finalSessionId));
				}

				// LangGraph 스트리밍 실행
				AgentState finalState = agentGraph.executeStreaming(initialState, message, emitter);

				// 에러 처리
				if (finalState.getError() != null && !finalState.getError().isEmpty()) {
					emitter.send(SseEmitter.event()
							.name("error")
							.data(finalState.getError()));
					emitter.complete();
					return;
				}

				// 답변 검수 (비동기로 수행, 스트리밍에는 영향 없음)
				finalState = validationNode.validate(finalState);

				// 대화 히스토리 저장
				saveToHistory(finalSessionId, userMessage, finalState.getAiMessage());

				// 세션 저장
				sessionStore.saveSession(finalSessionId, finalState);

				// 검수 결과 전송 (있는 경우)
				if (finalState.getMetadata().containsKey("validationScore")) {
					ChatV2Response.ValidationResult validation = buildValidationResult(finalState);
					emitter.send(SseEmitter.event()
							.name("validation")
							.data(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(validation)));
				}

				// 완료 이벤트
				double executionTime = (System.currentTimeMillis() - startTime) / 1000.0;
				emitter.send(SseEmitter.event()
						.name("complete")
						.data("채팅이 완료되었습니다. 실행 시간: " + executionTime + "초"));

				emitter.complete();

				log.info("ChatV2Service: 스트리밍 채팅 완료 - sessionId: {}, 실행 시간: {}초", 
						finalSessionId, executionTime);

			} catch (Exception e) {
				log.error("ChatV2Service: 스트리밍 채팅 중 오류 발생", e);
				try {
					emitter.send(SseEmitter.event()
							.name("error")
							.data("채팅 중 오류가 발생했습니다: " + e.getMessage()));
					emitter.complete();
				} catch (java.io.IOException ioException) {
					log.error("ChatV2Service: 에러 이벤트 전송 실패", ioException);
					emitter.completeWithError(ioException);
				}
			}
		});

		return emitter;
	}

	/**
	 * System Instruction 가져오기 (비어있으면 기본값 파일에서 읽기)
	 * 
	 * @param providedSystemInstruction 사용자가 제공한 System Instruction (null 또는 빈 문자열 가능)
	 * @return System Instruction 문자열
	 */
	private String getSystemInstruction(String providedSystemInstruction) {
		// 사용자가 제공한 System Instruction이 있으면 사용
		if (providedSystemInstruction != null && !providedSystemInstruction.trim().isEmpty()) {
			log.debug("ChatV2Service: 사용자 제공 System Instruction 사용");
			return providedSystemInstruction.trim();
		}
		
		// 비어있으면 기본값 파일에서 읽기
		try {
			Resource resource = resourceLoader.getResource("classpath:prompts/default-system-instruction.txt");
			if (resource.exists()) {
				try (InputStream inputStream = resource.getInputStream()) {
					String defaultInstruction = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
					log.debug("ChatV2Service: 기본 System Instruction 파일에서 로드");
					return defaultInstruction.trim();
				}
			} else {
				log.warn("ChatV2Service: 기본 System Instruction 파일을 찾을 수 없습니다: prompts/default-system-instruction.txt");
			}
		} catch (IOException e) {
			log.error("ChatV2Service: 기본 System Instruction 파일 읽기 실패", e);
		}
		
		// 파일을 읽을 수 없으면 하드코딩된 기본값 사용
		log.debug("ChatV2Service: 하드코딩된 기본 System Instruction 사용");
		return "당신은 친절하고 도움이 되는 AI 어시스턴트입니다. 사용자의 질문에 정확하고 유용한 답변을 한국어로 제공하세요.";
	}

	/**
	 * 세션 로드 또는 생성
	 */
	private AgentState loadOrCreateSession(String sessionId, String systemInstruction) {
		AgentState state = sessionStore.loadSession(sessionId);
		
		if (state == null) {
			// 새 세션 생성
			state = new AgentState();
			state.setSessionId(sessionId);
			state.setSystemInstruction(systemInstruction);
			log.debug("ChatV2Service: 새 AgentState 생성 - sessionId: {}", sessionId);
		} else {
			// 기존 세션 로드
			// 대화 히스토리 로드
			java.util.List<Object> history = sessionStore.getHistory(sessionId);
			state.setMessages(history);
			
			// System Instruction 업데이트 (새로 제공된 경우)
			if (systemInstruction != null && !systemInstruction.isBlank()) {
				state.setSystemInstruction(systemInstruction);
			}
			
			log.debug("ChatV2Service: 기존 세션 로드 - sessionId: {}, 히스토리 크기: {}", 
					sessionId, history.size());
		}
		
		return state;
	}

	/**
	 * 대화 히스토리에 추가
	 */
	private void saveToHistory(String sessionId, UserMessage userMessage, AiMessage aiMessage) {
		sessionStore.addToHistory(sessionId, userMessage, aiMessage);
	}

	/**
	 * 응답 생성
	 */
	private ChatV2Response buildResponse(AgentState state, String sessionId, long startTime) {
		double executionTime = (System.currentTimeMillis() - startTime) / 1000.0;
		
		String response = state.getAiMessage() != null ? state.getAiMessage().text() : "";
		
		ChatV2Response.ChatV2ResponseBuilder builder = ChatV2Response.builder()
				.response(response)
				.sessionId(sessionId)
				.executionTime(executionTime)
				.metadata(state.getMetadata());

		// 검수 결과 추가 (있는 경우)
		if (state.getMetadata().containsKey("validationScore")) {
			ChatV2Response.ValidationResult validation = buildValidationResult(state);
			builder.validation(validation);
		}

		return builder.build();
	}

	/**
	 * 검수 결과 생성
	 */
	private ChatV2Response.ValidationResult buildValidationResult(AgentState state) {
		Object scoreObj = state.getMetadata().get("validationScore");
		Object passedObj = state.getMetadata().get("validationPassed");
		Object feedbackObj = state.getMetadata().get("validationFeedback");
		Object needsRegenerationObj = state.getMetadata().get("validationNeedsRegeneration");

		Double score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : null;
		Boolean passed = passedObj instanceof Boolean ? (Boolean) passedObj : null;
		String feedback = feedbackObj != null ? feedbackObj.toString() : null;
		Boolean needsRegeneration = needsRegenerationObj instanceof Boolean ? (Boolean) needsRegenerationObj : null;

		return ChatV2Response.ValidationResult.builder()
				.score(score)
				.passed(passed)
				.feedback(feedback)
				.needsRegeneration(needsRegeneration)
				.build();
	}

	/**
	 * 세션 ID 생성
	 */
	private String generateSessionId() {
		return "session-" + UUID.randomUUID().toString();
	}

	/**
	 * 채팅 예외
	 */
	public static class ChatV2Exception extends RuntimeException {
		private final String errorCode;

		public ChatV2Exception(String errorCode, String message) {
			super(message);
			this.errorCode = errorCode;
		}

		public String getErrorCode() {
			return errorCode;
		}
	}
}
