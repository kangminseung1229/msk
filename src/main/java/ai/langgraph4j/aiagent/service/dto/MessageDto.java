package ai.langgraph4j.aiagent.service.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메시지 직렬화를 위한 DTO
 * UserMessage와 AiMessage를 Redis에 저장하기 위한 간단한 형태로 변환
 */
@Data
@NoArgsConstructor
public class MessageDto {
	
	@JsonProperty("type")
	private String type; // "USER" or "AI"
	
	@JsonProperty("text")
	private String text;
	
	@JsonCreator
	public MessageDto(@JsonProperty("type") String type, @JsonProperty("text") String text) {
		this.type = type;
		this.text = text;
	}
	
	public static MessageDto fromUserMessage(dev.langchain4j.data.message.UserMessage userMessage) {
		if (userMessage == null) {
			return null;
		}
		return new MessageDto("USER", userMessage.singleText());
	}
	
	public static MessageDto fromAiMessage(dev.langchain4j.data.message.AiMessage aiMessage) {
		if (aiMessage == null) {
			return null;
		}
		return new MessageDto("AI", aiMessage.text());
	}
	
	public dev.langchain4j.data.message.UserMessage toUserMessage() {
		if ("USER".equals(type)) {
			return new dev.langchain4j.data.message.UserMessage(text);
		}
		return null;
	}
	
	public dev.langchain4j.data.message.AiMessage toAiMessage() {
		if ("AI".equals(type)) {
			return new dev.langchain4j.data.message.AiMessage(text);
		}
		return null;
	}
}
