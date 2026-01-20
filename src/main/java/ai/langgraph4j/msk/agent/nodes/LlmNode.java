package ai.langgraph4j.msk.agent.nodes;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import ai.langgraph4j.msk.agent.state.AgentState;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * LLM 호출 노드
 * Spring AI ChatModel을 사용하여 LLM을 호출하고 응답을 생성합니다.
 * 
 * AgentGraph에서 사용되며, Gemini API를 통해 LLM 응답을 생성합니다.
 * 
 * @Qualifier("chatModel")을 사용하여 AiConfig에서 생성한 chatModel Bean을 명시적으로 지정합니다.
 */
@Slf4j
@Component
public class LlmNode {

	private final ChatModel chatModel;

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
		log.debug("LlmNode: LLM 호출 시작");

		try {
			// 대화 히스토리 준비
			List<Message> messages = prepareMessages(state);

			// LLM 호출
			Prompt prompt = new Prompt(messages);
			ChatResponse response = chatModel.call(prompt);

			// 응답 추출
			String content = response.getResult().getOutput().getText();
			AiMessage aiMessage = new AiMessage(content);

			// 도구 실행 요청 추출 (간단한 구현, 실제로는 더 복잡할 수 있음)
			List<ToolExecutionRequest> toolRequests = extractToolRequests(aiMessage);

			// 상태 업데이트
			state.setAiMessage(aiMessage);
			state.setToolExecutionRequests(toolRequests);
			state.getMessages().add(aiMessage);
			state.setCurrentStep("llm");
			state.incrementIterationCount();

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
	 */
	private List<Message> prepareMessages(AgentState state) {
		List<Message> messages = new ArrayList<>();

		// 사용자 메시지가 있으면 추가
		if (state.getUserMessage() != null) {
			messages.add(new org.springframework.ai.chat.messages.UserMessage(
					state.getUserMessage().text()));
		}

		// 도구 실행 결과가 있으면 추가
		if (!state.getToolExecutionResults().isEmpty()) {
			StringBuilder toolResults = new StringBuilder("도구 실행 결과:\n");
			for (AgentState.ToolExecutionResult result : state.getToolExecutionResults()) {
				toolResults.append("- ").append(result.getToolName())
						.append(": ").append(result.getResult()).append("\n");
			}
			messages.add(new org.springframework.ai.chat.messages.UserMessage(toolResults.toString()));
		}

		return messages;
	}

	/**
	 * AI 메시지에서 도구 실행 요청 추출
	 * (간단한 구현, 실제로는 LangChain4j의 ToolExecutionRequest 파싱 필요)
	 */
	private List<ToolExecutionRequest> extractToolRequests(AiMessage aiMessage) {
		// 실제 구현에서는 AiMessage에서 ToolExecutionRequest를 추출해야 함
		// 현재는 빈 리스트 반환 (Phase 3에서 구현 예정)
		return new ArrayList<>();
	}
}
