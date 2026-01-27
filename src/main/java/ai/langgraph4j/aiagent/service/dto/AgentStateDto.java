package ai.langgraph4j.aiagent.service.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ai.langgraph4j.aiagent.agent.state.AgentState;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AgentState 직렬화를 위한 DTO
 * UserMessage와 AiMessage를 DTO로 변환하여 Redis에 저장
 */
@Data
@NoArgsConstructor
public class AgentStateDto {
	
	private MessageDto userMessage;
	private MessageDto aiMessage;
	private List<ToolExecutionRequest> toolExecutionRequests = new ArrayList<>();
	private List<AgentState.ToolExecutionResult> toolExecutionResults = new ArrayList<>();
	private List<MessageDto> messages = new ArrayList<>();
	private String currentStep;
	private int iterationCount = 0;
	private String sessionId;
	private String error;
	private Map<String, Object> metadata = new HashMap<>();
	private String systemInstruction;
	
	/**
	 * AgentState를 DTO로 변환
	 */
	public static AgentStateDto fromAgentState(AgentState state) {
		if (state == null) {
			return null;
		}
		
		AgentStateDto dto = new AgentStateDto();
		dto.setUserMessage(state.getUserMessage() != null ? 
			MessageDto.fromUserMessage(state.getUserMessage()) : null);
		dto.setAiMessage(state.getAiMessage() != null ? 
			MessageDto.fromAiMessage(state.getAiMessage()) : null);
		dto.setToolExecutionRequests(state.getToolExecutionRequests() != null ? 
			new ArrayList<>(state.getToolExecutionRequests()) : new ArrayList<>());
		dto.setToolExecutionResults(state.getToolExecutionResults() != null ? 
			new ArrayList<>(state.getToolExecutionResults()) : new ArrayList<>());
		
		// messages 리스트를 MessageDto로 변환
		if (state.getMessages() != null) {
			List<MessageDto> messageDtos = new ArrayList<>();
			for (Object msg : state.getMessages()) {
				if (msg instanceof UserMessage userMsg) {
					messageDtos.add(MessageDto.fromUserMessage(userMsg));
				} else if (msg instanceof AiMessage aiMsg) {
					messageDtos.add(MessageDto.fromAiMessage(aiMsg));
				}
			}
			dto.setMessages(messageDtos);
		}
		
		dto.setCurrentStep(state.getCurrentStep());
		dto.setIterationCount(state.getIterationCount());
		dto.setSessionId(state.getSessionId());
		dto.setError(state.getError());
		dto.setMetadata(state.getMetadata() != null ? 
			new HashMap<>(state.getMetadata()) : new HashMap<>());
		dto.setSystemInstruction(state.getSystemInstruction());
		
		return dto;
	}
	
	/**
	 * DTO를 AgentState로 변환
	 */
	@JsonIgnore
	public AgentState toAgentState() {
		AgentState state = new AgentState();
		
		state.setUserMessage(this.userMessage != null ? 
			this.userMessage.toUserMessage() : null);
		state.setAiMessage(this.aiMessage != null ? 
			this.aiMessage.toAiMessage() : null);
		state.setToolExecutionRequests(this.toolExecutionRequests != null ? 
			new ArrayList<>(this.toolExecutionRequests) : new ArrayList<>());
		state.setToolExecutionResults(this.toolExecutionResults != null ? 
			new ArrayList<>(this.toolExecutionResults) : new ArrayList<>());
		
		// MessageDto 리스트를 UserMessage/AiMessage로 변환
		if (this.messages != null) {
			List<Object> messageList = new ArrayList<>();
			for (MessageDto dto : this.messages) {
				if ("USER".equals(dto.getType())) {
					messageList.add(dto.toUserMessage());
				} else if ("AI".equals(dto.getType())) {
					messageList.add(dto.toAiMessage());
				}
			}
			state.setMessages(messageList);
		}
		
		// Exception은 직렬화할 수 없으므로 null로 설정 (에러 메시지는 error 필드에 저장됨)
		state.setException(null);
		
		state.setCurrentStep(this.currentStep);
		state.setIterationCount(this.iterationCount);
		state.setSessionId(this.sessionId);
		state.setError(this.error);
		state.setMetadata(this.metadata != null ? 
			new HashMap<>(this.metadata) : new HashMap<>());
		state.setSystemInstruction(this.systemInstruction);
		
		return state;
	}
}
