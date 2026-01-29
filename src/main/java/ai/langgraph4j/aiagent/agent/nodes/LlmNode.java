package ai.langgraph4j.aiagent.agent.nodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.langgraph4j.aiagent.agent.state.AgentState;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * LLM 호출 노드
 * Spring AI ChatModel을 사용하여 LLM을 호출하고 응답을 생성합니다.
 * 
 * Phase 3: Spring AI Tool 자동 호출을 활용합니다.
 * Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달하므로,
 * 수동 Tool 파싱 로직이 필요 없습니다.
 * 
 * AgentGraph에서 사용되며, Gemini API를 통해 LLM 응답을 생성합니다.
 * 
 * @Qualifier("chatModel")을 사용하여 AiConfig에서 생성한 chatModel Bean을 명시적으로 지정합니다.
 */
@Slf4j
@Component
public class LlmNode {

	private final ChatModel chatModel;

	@Value("${agent.max-iterations:5}")
	private int maxIterations;

	public LlmNode(@Qualifier("chatModel") ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * LLM을 호출하여 응답 생성
	 * 
	 * @param state 현재 상태
	 * @return 업데이트된 상태
	 */
	public AgentState process(AgentState state) {
		log.debug("LlmNode: LLM 호출 시작, 현재 반복 횟수: {}", state.getIterationCount());

		// LLM 호출 횟수 제한 체크
		if (state.getIterationCount() >= maxIterations) {
			log.warn("LlmNode: 최대 LLM 호출 횟수 초과 ({}), LLM 호출을 건너뜁니다", maxIterations);
			state.setError("최대 LLM 호출 횟수(" + maxIterations + "회)를 초과했습니다. 요청이 너무 복잡합니다.");
			state.setCurrentStep("error");
			return state;
		}

		try {
			// 반복 횟수 증가 (LLM 호출 전에 증가)
			state.incrementIterationCount();

			// 대화 히스토리 준비
			List<Message> messages = prepareMessages(state);

			// Phase 3: Spring AI Tool 자동 호출
			// ChatModel에 Tool이 이미 통합되어 있으므로, Spring AI가 자동으로 Tool을 호출합니다.
			// Tool 호출이 필요한 경우 Spring AI가 자동으로 처리하고 결과를 LLM에 전달합니다.
			Prompt prompt = new Prompt(messages);

			// 토큰 수 추정 및 로깅 (LLM 호출 전)
			long estimatedInputTokens = estimateTokens(prompt);
			log.info("LlmNode: LLM 호출 전 토큰 추정 - 입력 토큰: {}개 (메시지 수: {}개)", 
					estimatedInputTokens, messages.size());
			ChatResponse response = chatModel.call(prompt);

			// 실제 토큰 사용량 확인 및 로깅 (LLM 호출 후)
			logTokenUsage(response, estimatedInputTokens);

			// 응답 추출
			// Spring AI가 Tool을 자동으로 호출했을 수 있으므로, 최종 응답을 확인합니다.
			String content = response.getResult().getOutput().getText();
			AiMessage aiMessage = new AiMessage(content);

			// Phase 3: Spring AI가 자동으로 Tool을 호출하므로 수동 Tool 파싱이 필요 없습니다.
			// Tool 호출이 필요한 경우 Spring AI가 자동으로 처리하고 결과를 LLM에 전달합니다.
			// 따라서 Tool 요청 추출 로직을 제거하고, Spring AI의 자동 Tool 호출을 활용합니다.
			List<ToolExecutionRequest> toolRequests = new ArrayList<>();

			// 상태 업데이트
			state.setAiMessage(aiMessage);
			state.setToolExecutionRequests(toolRequests);
			state.getMessages().add(aiMessage);
			state.setCurrentStep("llm");

			log.debug("LlmNode: LLM 호출 완료, 반복 횟수: {}", state.getIterationCount());

			return state;
		} catch (NonTransientAiException e) {
			log.error("LlmNode: LLM 호출 실패 (NonTransient)", e);

			// 예외 메시지에서 quota 관련 키워드 확인
			String errorMessage = e.getMessage();
			if (errorMessage != null && (errorMessage.contains("quota") ||
					errorMessage.contains("insufficient_quota") ||
					errorMessage.contains("429") ||
					errorMessage.contains("exceeded"))) {
				String errorMsg = "Gemini API 할당량이 초과되었습니다. API 키의 사용량을 확인하거나 결제 정보를 확인해주세요.";
				log.error("LlmNode: Gemini API 할당량 초과 - {}", errorMessage);
				state.setError(errorMsg);
			} else {
				state.setError("LLM 호출 중 오류 발생: " + errorMessage);
			}
			state.setException(e);
			state.setCurrentStep("error");
			return state;
		} catch (Exception e) {
			log.error("LlmNode: LLM 호출 실패", e);

			// 예외 메시지에서 quota 관련 키워드 확인
			String errorMessage = e.getMessage();
			if (errorMessage != null && (errorMessage.contains("quota") ||
					errorMessage.contains("insufficient_quota") ||
					errorMessage.contains("429") ||
					errorMessage.contains("exceeded"))) {
				state.setError("Gemini API 할당량이 초과되었습니다. API 키의 사용량을 확인하거나 결제 정보를 확인해주세요.");
			} else {
				state.setError("LLM 호출 중 오류 발생: " + errorMessage);
			}
			state.setException(e);
			state.setCurrentStep("error");
			return state;
		}
	}

	/**
	 * 대화 히스토리를 Spring AI Message 리스트로 변환
	 * 
	 * v2: 대화 히스토리 지원
	 * state.getMessages()에 저장된 이전 대화 히스토리를 포함하여 멀티 턴 대화를 지원합니다.
	 * 
	 * Phase 3: Spring AI Tool 자동 호출
	 * Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달하므로,
	 * Tool 실행 결과를 수동으로 추가할 필요가 없습니다.
	 */
	private List<Message> prepareMessages(AgentState state) {
		List<Message> messages = new ArrayList<>();

		// System Instruction이 있으면 먼저 추가
		if (state.getSystemInstruction() != null && !state.getSystemInstruction().trim().isEmpty()) {
			messages.add(new org.springframework.ai.chat.messages.SystemMessage(
					state.getSystemInstruction()));
		} else {
			// System Instruction이 없으면 한국어 기본 메시지 추가
			messages.add(new org.springframework.ai.chat.messages.SystemMessage(
					"당신은 친절하고 도움이 되는 AI 어시스턴트입니다. 사용자의 질문에 정확하고 유용한 답변을 한국어로 제공하세요."));
		}

		// v2: 이전 대화 히스토리 추가 (state.getMessages()에 저장된 메시지들)
		// state.getMessages()에는 dev.langchain4j.data.message.UserMessage와 AiMessage가 저장됨
		if (state.getMessages() != null && !state.getMessages().isEmpty()) {
			for (Object msg : state.getMessages()) {
				if (msg instanceof UserMessage userMsg) {
					// LangChain4j UserMessage를 Spring AI UserMessage로 변환
					messages.add(new org.springframework.ai.chat.messages.UserMessage(userMsg.singleText()));
				} else if (msg instanceof AiMessage aiMsg) {
					// LangChain4j AiMessage를 Spring AI AssistantMessage로 변환
					// Spring AI에서는 AiMessage가 아니라 AssistantMessage를 사용함
					messages.add(new org.springframework.ai.chat.messages.AssistantMessage(aiMsg.text()));
				}
				// 다른 타입의 메시지는 무시
			}
		}

		// 현재 사용자 메시지 추가 (히스토리에 아직 추가되지 않은 새 메시지)
		// state.getUserMessage()는 dev.langchain4j.data.message.UserMessage 타입
		if (state.getUserMessage() != null) {
			messages.add(new org.springframework.ai.chat.messages.UserMessage(
					state.getUserMessage().singleText()));
		}

		log.debug("LlmNode: 메시지 준비 완료 - System: 1개, 히스토리: {}개, 현재 사용자: {}개, 전체: {}개",
				state.getMessages() != null ? state.getMessages().size() : 0,
				state.getUserMessage() != null ? 1 : 0,
				messages.size());

		return messages;
	}

	/**
	 * Phase 3: 스트리밍 모드로 LLM 호출
	 * StreamingChatModel을 사용하여 실시간으로 응답을 생성하고 SSE로 전송합니다.
	 * 
	 * @param state 현재 상태
	 * @param emitter SSE emitter (스트리밍 응답 전송용)
	 * @return 업데이트된 상태
	 */
	public AgentState processStreaming(AgentState state, SseEmitter emitter) {
		log.debug("LlmNode: 스트리밍 모드로 LLM 호출 시작, 현재 반복 횟수: {}", state.getIterationCount());

		// LLM 호출 횟수 제한 체크
		if (state.getIterationCount() >= maxIterations) {
			log.warn("LlmNode: 최대 LLM 호출 횟수 초과 ({}), LLM 호출을 건너뜁니다", maxIterations);
			state.setError("최대 LLM 호출 횟수(" + maxIterations + "회)를 초과했습니다. 요청이 너무 복잡합니다.");
			state.setCurrentStep("error");
			try {
				emitter.send(SseEmitter.event()
						.name("error")
						.data("최대 LLM 호출 횟수를 초과했습니다."));
			} catch (IOException e) {
				log.error("LlmNode: 에러 이벤트 전송 실패", e);
			}
			return state;
		}

		try {
			// 반복 횟수 증가 (LLM 호출 전에 증가)
			state.incrementIterationCount();

			// 대화 히스토리 준비
			List<Message> messages = prepareMessages(state);

			// StreamingChatModel 지원 확인
			if (chatModel instanceof StreamingChatModel) {
				@SuppressWarnings("unchecked")
				StreamingChatModel streamingChatModel = (StreamingChatModel) chatModel;
				Prompt prompt = new Prompt(messages);

				// 토큰 수 추정 및 로깅 (LLM 호출 전)
				long estimatedInputTokens = estimateTokens(prompt);
				log.info("LlmNode: 스트리밍 LLM 호출 전 토큰 추정 - 입력 토큰: {}개 (메시지 수: {}개)", 
						estimatedInputTokens, messages.size());

				// 스트리밍 응답 수집
				AtomicReference<StringBuilder> fullResponse = new AtomicReference<>(new StringBuilder());
				AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();
				// 이전 텍스트를 추적하여 델타만 전송 (중복 전송 방지)
				AtomicReference<String> previousText = new AtomicReference<>("");

				Flux<ChatResponse> responseFlux = streamingChatModel.stream(prompt);

				// 스트리밍 응답 처리
				// 각 청크는 doOnNext에서 즉시 SSE로 전송되므로 스트리밍이 정상 작동합니다.
				// blockLast()는 스트리밍 완료까지 대기하지만, 각 청크는 즉시 전송됩니다.
				responseFlux
						.doOnNext(chatResponse -> {
							try {
								// ChatResponse 검증
								if (chatResponse == null || chatResponse.getResult() == null 
										|| chatResponse.getResult().getOutput() == null) {
									log.debug("LlmNode: 스트리밍 응답에 result 또는 output이 없습니다 (Tool 호출 중이거나 중간 응답일 수 있음)");
									return;
								}

								String currentText = chatResponse.getResult().getOutput().getText();
								if (currentText == null || currentText.isEmpty()) {
									log.debug("LlmNode: 스트리밍 응답에 텍스트가 없습니다 (중간 응답일 수 있음)");
									return;
								}

								// 전체 응답 누적
								fullResponse.get().append(currentText);
								lastResponse.set(chatResponse);

								// 델타만 추출 (현재 텍스트에서 이전 텍스트 제거하여 중복 전송 방지)
								String previous = previousText.get();
								String delta = currentText;
								if (currentText.startsWith(previous)) {
									delta = currentText.substring(previous.length());
								}

								// 델타가 있으면 SSE로 즉시 전송 (버퍼링 없이)
								if (!delta.isEmpty()) {
									emitter.send(SseEmitter.event()
											.name("chunk")
											.data(delta));
									previousText.set(currentText);
								}
							} catch (IOException e) {
								log.error("LlmNode: 스트리밍 청크 전송 중 오류", e);
							} catch (Exception e) {
								log.error("LlmNode: 스트리밍 응답 처리 중 오류 발생 (계속 진행)", e);
							}
						})
						.doOnError(error -> {
							log.error("LlmNode: 스트리밍 중 오류 발생", error);
							try {
								emitter.send(SseEmitter.event()
										.name("error")
										.data("LLM 스트리밍 중 오류: " + error.getMessage()));
							} catch (IOException e) {
								log.error("LlmNode: 에러 이벤트 전송 실패", e);
							}
						})
						.doOnComplete(() -> log.debug("LlmNode: 스트리밍 완료"))
						.blockLast(); // 스트리밍 완료까지 대기 (각 청크는 doOnNext에서 즉시 전송됨)

				// 최종 응답 설정
				String finalContent = fullResponse.get().toString();
				AiMessage aiMessage = new AiMessage(finalContent);

				// 상태 업데이트
				state.setAiMessage(aiMessage);
				state.setToolExecutionRequests(new ArrayList<>());
				state.getMessages().add(aiMessage);
				state.setCurrentStep("llm");

				// 실제 토큰 사용량 확인 및 로깅 (스트리밍 응답의 경우)
				ChatResponse finalResponse = lastResponse.get();
				if (finalResponse != null) {
					logTokenUsage(finalResponse, estimatedInputTokens);
				} else {
					// 응답이 없으면 추정값만 로깅
					long estimatedOutputTokens = estimateTokensFromText(finalContent);
					log.info("LlmNode: 스트리밍 LLM 호출 완료 - 입력 토큰: {}개 (추정), 출력 토큰: {}개 (추정), 총: {}개 (추정), 응답 길이: {}자",
							estimatedInputTokens, estimatedOutputTokens, 
							estimatedInputTokens + estimatedOutputTokens, finalContent.length());
				}

				log.debug("LlmNode: 스트리밍 LLM 호출 완료, 반복 횟수: {}, 응답 길이: {}자",
						state.getIterationCount(), finalContent.length());

			} else {
				// 스트리밍 미지원 - 일반 호출 사용
				log.warn("LlmNode: ChatModel이 StreamingChatModel을 구현하지 않음. 일반 호출 사용");
				emitter.send(SseEmitter.event()
						.name("info")
						.data("스트리밍을 지원하지 않아 일반 모드로 실행합니다."));

				// 일반 process 메서드 호출
				state = process(state);
			}

			return state;

		} catch (NonTransientAiException e) {
			log.error("LlmNode: LLM 호출 실패 (NonTransient)", e);

			String errorMessage = e.getMessage();
			if (errorMessage != null && (errorMessage.contains("quota") ||
					errorMessage.contains("insufficient_quota") ||
					errorMessage.contains("429") ||
					errorMessage.contains("exceeded"))) {
				String errorMsg = "Gemini API 할당량이 초과되었습니다. API 키의 사용량을 확인하거나 결제 정보를 확인해주세요.";
				log.error("LlmNode: Gemini API 할당량 초과 - {}", errorMessage);
				state.setError(errorMsg);
			} else {
				state.setError("LLM 호출 중 오류 발생: " + errorMessage);
			}
			state.setException(e);
			state.setCurrentStep("error");

			try {
				emitter.send(SseEmitter.event()
						.name("error")
						.data(state.getError()));
			} catch (IOException ioException) {
				log.error("LlmNode: 에러 이벤트 전송 실패", ioException);
			}

			return state;
		} catch (Exception e) {
			log.error("LlmNode: LLM 호출 실패", e);

			String errorMessage = e.getMessage();
			if (errorMessage != null && (errorMessage.contains("quota") ||
					errorMessage.contains("insufficient_quota") ||
					errorMessage.contains("429") ||
					errorMessage.contains("exceeded"))) {
				state.setError("Gemini API 할당량이 초과되었습니다. API 키의 사용량을 확인하거나 결제 정보를 확인해주세요.");
			} else {
				state.setError("LLM 호출 중 오류 발생: " + errorMessage);
			}
			state.setException(e);
			state.setCurrentStep("error");

			try {
				emitter.send(SseEmitter.event()
						.name("error")
						.data(state.getError()));
			} catch (IOException ioException) {
				log.error("LlmNode: 에러 이벤트 전송 실패", ioException);
			}

			return state;
		}
	}

	/**
	 * Prompt의 토큰 수를 추정
	 * 한국어 중심 텍스트 기준: 1 토큰 ≈ 2.5 문자 (보수적 추정)
	 * 
	 * @param prompt Prompt 객체
	 * @return 추정된 토큰 수
	 */
	private long estimateTokens(Prompt prompt) {
		if (prompt == null || prompt.getInstructions() == null || prompt.getInstructions().isEmpty()) {
			return 0;
		}

		// Prompt의 전체 텍스트 길이 추정
		long totalChars = 0;
		for (Message msg : prompt.getInstructions()) {
			try {
				// Message의 toString()으로 대략적인 길이 추정
				// 실제로는 Message의 내용을 정확히 추출해야 하지만,
				// 토큰 추정 목적이므로 Prompt의 toString() 사용
				String text = msg.toString();
				if (text != null && text.length() > 20) { // 클래스명보다 긴 경우만 카운트
					totalChars += text.length();
				}
			} catch (Exception e) {
				// 메시지 파싱 실패 시 기본값 사용
				totalChars += 100;
			}
		}

		// Prompt 전체를 문자열로 변환하여 더 정확한 추정
		try {
			String promptText = prompt.toString();
			if (promptText != null && promptText.length() > totalChars) {
				totalChars = promptText.length();
			}
		} catch (Exception e) {
			// 무시하고 계속 진행
		}

		// 한국어 중심 텍스트: 1 토큰 ≈ 2.5 문자 (보수적 추정)
		// 영어는 1 토큰 ≈ 4 문자, 한국어는 1 토큰 ≈ 2-3 문자
		// 혼합 텍스트를 고려하여 2.5로 설정
		return Math.round(totalChars / 2.5);
	}

	/**
	 * 텍스트의 토큰 수를 추정
	 * 
	 * @param text 텍스트
	 * @return 추정된 토큰 수
	 */
	private long estimateTokensFromText(String text) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		return Math.round(text.length() / 2.5);
	}

	/**
	 * ChatResponse에서 실제 토큰 사용량을 확인하고 로깅
	 * 
	 * @param response ChatResponse
	 * @param estimatedInputTokens 추정된 입력 토큰 수
	 */
	private void logTokenUsage(ChatResponse response, long estimatedInputTokens) {
		if (response == null) {
			return;
		}

		try {
			Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;
			
			if (usage != null) {
				// 실제 토큰 사용량이 있는 경우
				Integer promptTokensInt = usage.getPromptTokens();
				Integer totalTokensInt = usage.getTotalTokens();
				
				Long promptTokens = promptTokensInt != null ? Long.valueOf(promptTokensInt) : null;
				Long totalTokens = totalTokensInt != null ? Long.valueOf(totalTokensInt) : null;
				
				// 출력 토큰 수 계산 (총 토큰 - 입력 토큰)
				Long completionTokens = null;
				if (promptTokens != null && totalTokens != null) {
					completionTokens = totalTokens - promptTokens;
				}

				log.info("LlmNode: LLM 토큰 사용량 - 입력: {}개, 출력: {}개, 총: {}개 (실제 사용량)", 
						promptTokens != null ? promptTokens : "N/A",
						completionTokens != null ? completionTokens : "N/A",
						totalTokens != null ? totalTokens : "N/A");

				// 추정값과 실제값 비교
				if (promptTokens != null && estimatedInputTokens > 0) {
					long diff = Math.abs(promptTokens - estimatedInputTokens);
					double diffPercent = (diff * 100.0) / estimatedInputTokens;
					log.debug("LlmNode: 토큰 추정 정확도 - 추정: {}개, 실제: {}개, 차이: {}개 ({}%)", 
							estimatedInputTokens, promptTokens, diff, String.format("%.1f", diffPercent));
				}
			} else {
				// Usage 메타데이터가 없는 경우 (일부 모델은 제공하지 않음)
				log.info("LlmNode: LLM 토큰 사용량 - 입력: {}개 (추정값, 실제 사용량 정보 없음)", estimatedInputTokens);
			}
		} catch (Exception e) {
			log.debug("LlmNode: 토큰 사용량 확인 중 오류 (무시하고 계속 진행): {}", e.getMessage());
		}
	}

	/**
	 * Phase 3: Spring AI Tool 자동 호출
	 * 
	 * Phase 2의 수동 Tool 파싱 로직은 제거되었습니다.
	 * Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달하므로,
	 * 수동 Tool 파싱이 필요 없습니다.
	 */
}
