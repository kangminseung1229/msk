package ai.langgraph4j.aiagent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.langgraph4j.aiagent.agent.state.AgentState;
import ai.langgraph4j.aiagent.service.dto.AgentStateDto;
import ai.langgraph4j.aiagent.service.dto.MessageDto;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 세션 저장소
 * 대화 히스토리를 Redis에 저장하고 조회합니다.
 * 
 * v2: LangGraph 기반 채팅 시스템의 세션 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionStore {

	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;
	
	private static final String SESSION_KEY_PREFIX = "chat:session:";
	private static final String HISTORY_KEY_PREFIX = "chat:history:";
	private static final long SESSION_TTL_HOURS = 24; // 세션 만료 시간: 24시간

	/**
	 * 세션의 AgentState를 Redis에 저장
	 * 
	 * @param sessionId 세션 ID
	 * @param state AgentState
	 */
	public void saveSession(String sessionId, AgentState state) {
		if (sessionId == null || sessionId.isBlank()) {
			log.warn("SessionStore: 세션 ID가 없어 저장을 건너뜁니다");
			return;
		}

		try {
			String sessionKey = SESSION_KEY_PREFIX + sessionId;
			
			// AgentState를 DTO로 변환하여 저장
			AgentStateDto stateDto = AgentStateDto.fromAgentState(state);
			String stateJson = objectMapper.writeValueAsString(stateDto);
			
			redisTemplate.opsForValue().set(sessionKey, stateJson, SESSION_TTL_HOURS, TimeUnit.HOURS);
			log.debug("SessionStore: 세션 저장 완료 - sessionId: {}", sessionId);
		} catch (Exception e) {
			log.error("SessionStore: 세션 저장 중 오류 발생 - sessionId: {}", sessionId, e);
		}
	}

	/**
	 * 세션의 AgentState를 Redis에서 조회
	 * 
	 * @param sessionId 세션 ID
	 * @return AgentState (없으면 null)
	 */
	public AgentState loadSession(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			log.warn("SessionStore: 세션 ID가 없어 조회를 건너뜁니다");
			return null;
		}

		try {
			String sessionKey = SESSION_KEY_PREFIX + sessionId;
			String stateJson = redisTemplate.opsForValue().get(sessionKey);
			
			if (stateJson == null) {
				log.debug("SessionStore: 세션을 찾을 수 없음 - sessionId: {}", sessionId);
				return null;
			}

			// DTO로 읽어서 AgentState로 변환
			AgentStateDto stateDto = objectMapper.readValue(stateJson, AgentStateDto.class);
			AgentState state = stateDto != null ? stateDto.toAgentState() : null;
			
			log.debug("SessionStore: 세션 조회 완료 - sessionId: {}", sessionId);
			return state;
		} catch (Exception e) {
			log.error("SessionStore: 세션 조회 중 오류 발생 - sessionId: {}", sessionId, e);
			return null;
		}
	}

	/**
	 * 대화 히스토리를 Redis에 추가
	 * 
	 * @param sessionId 세션 ID
	 * @param userMessage 사용자 메시지
	 * @param aiMessage AI 메시지
	 */
	public void addToHistory(String sessionId, UserMessage userMessage, AiMessage aiMessage) {
		if (sessionId == null || sessionId.isBlank()) {
			log.warn("SessionStore: 세션 ID가 없어 히스토리 추가를 건너뜁니다");
			return;
		}

		try {
			String historyKey = HISTORY_KEY_PREFIX + sessionId;
			
			// 기존 히스토리 조회 (MessageDto 리스트)
			List<MessageDto> history = getHistoryAsDtos(sessionId);
			
			// 새 메시지를 DTO로 변환하여 추가
			if (userMessage != null) {
				history.add(MessageDto.fromUserMessage(userMessage));
			}
			if (aiMessage != null) {
				history.add(MessageDto.fromAiMessage(aiMessage));
			}
			
			// 히스토리 저장 (MessageDto 리스트를 JSON으로 직렬화)
			String historyJson = objectMapper.writeValueAsString(history);
			redisTemplate.opsForValue().set(historyKey, historyJson, SESSION_TTL_HOURS, TimeUnit.HOURS);
			
			log.debug("SessionStore: 히스토리 추가 완료 - sessionId: {}, 히스토리 크기: {}", sessionId, history.size());
		} catch (Exception e) {
			log.error("SessionStore: 히스토리 추가 중 오류 발생 - sessionId: {}", sessionId, e);
		}
	}

	/**
	 * 대화 히스토리를 Redis에서 조회 (MessageDto 리스트로 반환)
	 * 
	 * @param sessionId 세션 ID
	 * @return 대화 히스토리 DTO 리스트
	 */
	private List<MessageDto> getHistoryAsDtos(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			log.warn("SessionStore: 세션 ID가 없어 히스토리 조회를 건너뜁니다");
			return new ArrayList<>();
		}

		try {
			String historyKey = HISTORY_KEY_PREFIX + sessionId;
			String historyJson = redisTemplate.opsForValue().get(historyKey);
			
			if (historyJson == null) {
				log.debug("SessionStore: 히스토리를 찾을 수 없음 - sessionId: {}", sessionId);
				return new ArrayList<>();
			}

			// JSON을 List<MessageDto>로 변환
			TypeReference<List<MessageDto>> typeRef = new TypeReference<List<MessageDto>>() {};
			List<MessageDto> history = objectMapper.readValue(historyJson, typeRef);
			
			log.debug("SessionStore: 히스토리 조회 완료 - sessionId: {}, 히스토리 크기: {}", sessionId, history != null ? history.size() : 0);
			return history != null ? history : new ArrayList<>();
		} catch (Exception e) {
			log.error("SessionStore: 히스토리 조회 중 오류 발생 - sessionId: {}", sessionId, e);
			return new ArrayList<>();
		}
	}

	/**
	 * 대화 히스토리를 Redis에서 조회 (UserMessage, AiMessage 객체 리스트로 반환)
	 * 
	 * @param sessionId 세션 ID
	 * @return 대화 히스토리 리스트 (UserMessage, AiMessage 객체)
	 */
	public List<Object> getHistory(String sessionId) {
		List<MessageDto> dtos = getHistoryAsDtos(sessionId);
		
		// MessageDto를 UserMessage/AiMessage로 변환
		return dtos.stream()
			.map(dto -> {
				if ("USER".equals(dto.getType())) {
					return dto.toUserMessage();
				} else if ("AI".equals(dto.getType())) {
					return dto.toAiMessage();
				}
				return null;
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	/**
	 * 세션 삭제
	 * 
	 * @param sessionId 세션 ID
	 */
	public void deleteSession(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return;
		}

		try {
			String sessionKey = SESSION_KEY_PREFIX + sessionId;
			String historyKey = HISTORY_KEY_PREFIX + sessionId;
			
			redisTemplate.delete(sessionKey);
			redisTemplate.delete(historyKey);
			
			log.debug("SessionStore: 세션 삭제 완료 - sessionId: {}", sessionId);
		} catch (Exception e) {
			log.error("SessionStore: 세션 삭제 중 오류 발생 - sessionId: {}", sessionId, e);
		}
	}

	/**
	 * 세션 존재 여부 확인
	 * 
	 * @param sessionId 세션 ID
	 * @return 존재 여부
	 */
	public boolean exists(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return false;
		}

		try {
			String sessionKey = SESSION_KEY_PREFIX + sessionId;
			Boolean exists = redisTemplate.hasKey(sessionKey);
			return exists != null && exists;
		} catch (Exception e) {
			log.error("SessionStore: 세션 존재 확인 중 오류 발생 - sessionId: {}", sessionId, e);
			return false;
		}
	}
}
