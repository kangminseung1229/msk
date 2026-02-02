package ai.langgraph4j.aiagent.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.langgraph4j.aiagent.entity.chat.ChatMessage;
import ai.langgraph4j.aiagent.entity.chat.ChatSession;
import ai.langgraph4j.aiagent.repository.ChatMessageRepository;
import ai.langgraph4j.aiagent.repository.ChatSessionRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 세션 DB 영구 저장
 * 새 세션의 대화는 저장 버튼 없이 항상 DB에 저장됩니다.
 * test 프로파일에서는 JPA가 비활성화되므로 로드하지 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!test")
public class ChatSessionPersistenceService {

	private static final String DEFAULT_TITLE = "새 대화";
	private static final int TITLE_MAX_LENGTH = 200;

	private final ChatSessionRepository chatSessionRepository;
	private final ChatMessageRepository chatMessageRepository;

	@Value("${chat.session.persistence.enabled:true}")
	private boolean persistenceEnabled;

	/**
	 * 대화 1턴(사용자 메시지 + AI 메시지)을 DB에 저장
	 * 세션이 없으면 생성하고, 제목은 첫 사용자 메시지로 설정
	 */
	@Transactional
	public void persistTurn(String sessionId, UserMessage userMessage, AiMessage aiMessage) {
		if (!persistenceEnabled) {
			log.debug("ChatSessionPersistence: DB 저장 비활성화");
			return;
		}
		if (sessionId == null || sessionId.isBlank()) {
			log.warn("ChatSessionPersistence: sessionId 없음, 저장 건너뜀");
			return;
		}

		try {
			ChatSession session = chatSessionRepository.findBySessionId(sessionId)
					.orElseGet(() -> createNewSession(sessionId));

			int nextOrder = (int) chatMessageRepository.countByChatSession(session);
			if (userMessage != null && !userMessage.singleText().isBlank()) {
				String content = userMessage.singleText();
				if (session.getTitle() == null || session.getTitle().equals(DEFAULT_TITLE)) {
					session.setTitle(truncateTitle(content));
					chatSessionRepository.save(session);
				}
				ChatMessage userMsg = ChatMessage.builder()
						.chatSession(session)
						.role(ChatMessage.MessageRole.USER)
						.content(content)
						.sequenceOrder(nextOrder++)
						.build();
				chatMessageRepository.save(userMsg);
			}
			if (aiMessage != null && !aiMessage.text().isBlank()) {
				ChatMessage aiMsg = ChatMessage.builder()
						.chatSession(session)
						.role(ChatMessage.MessageRole.AI)
						.content(aiMessage.text())
						.sequenceOrder(nextOrder++)
						.build();
				chatMessageRepository.save(aiMsg);
			}
			// updatedAt 갱신
			chatSessionRepository.save(session);
			log.debug("ChatSessionPersistence: 세션 저장 완료 - sessionId: {}", sessionId);
		} catch (Exception e) {
			log.error("ChatSessionPersistence: DB 저장 실패 - sessionId: {}", sessionId, e);
			// 채팅 흐름은 Redis로 유지되므로 예외를 던지지 않고 로깅만
		}
	}

	private ChatSession createNewSession(String sessionId) {
		ChatSession session = ChatSession.builder()
				.sessionId(sessionId)
				.title(DEFAULT_TITLE)
				.build();
		return chatSessionRepository.save(session);
	}

	private static String truncateTitle(String text) {
		if (text == null || text.isBlank()) {
			return DEFAULT_TITLE;
		}
		String oneLine = text.replaceAll("\\s+", " ").trim();
		if (oneLine.length() <= TITLE_MAX_LENGTH) {
			return oneLine;
		}
		return oneLine.substring(0, TITLE_MAX_LENGTH) + "...";
	}

	/**
	 * 세션 목록 조회 (왼쪽 탭용, 최신순)
	 */
	@Transactional(readOnly = true)
	public List<ChatSessionDto> listSessions(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		return chatSessionRepository.findAllByOrderByUpdatedAtDesc(pageable).stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	/**
	 * 세션의 메시지 목록 조회
	 */
	@Transactional(readOnly = true)
	public Optional<ChatSessionWithMessagesDto> getSessionWithMessages(String sessionId) {
		return chatSessionRepository.findBySessionId(sessionId)
				.map(session -> {
					List<ChatMessage> messages = chatMessageRepository
							.findByChatSessionOrderBySequenceOrderAsc(session);
					return new ChatSessionWithMessagesDto(
							toDto(session),
							messages.stream()
									.map(m -> new ChatMessageDto(m.getRole().name(), m.getContent(),
											m.getSequenceOrder()))
									.collect(Collectors.toList()));
				});
	}

	private ChatSessionDto toDto(ChatSession s) {
		return new ChatSessionDto(s.getId(), s.getSessionId(), s.getTitle(), s.getCreatedAt(), s.getUpdatedAt());
	}

	/** 세션 목록용 DTO */
	public record ChatSessionDto(Long id, String sessionId, String title,
			java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
	}

	/** 메시지 DTO */
	public record ChatMessageDto(String role, String content, int sequenceOrder) {
	}

	/** 세션 + 메시지 목록 DTO */
	public record ChatSessionWithMessagesDto(ChatSessionDto session, List<ChatMessageDto> messages) {
	}
}
