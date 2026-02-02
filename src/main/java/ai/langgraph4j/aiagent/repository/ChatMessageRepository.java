package ai.langgraph4j.aiagent.repository;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.langgraph4j.aiagent.entity.chat.ChatMessage;
import ai.langgraph4j.aiagent.entity.chat.ChatSession;

/**
 * 채팅 메시지 Repository
 * test 프로파일에서는 JPA가 비활성화되므로 로드하지 않습니다.
 */
@Repository
@Profile("!test")
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findByChatSessionOrderBySequenceOrderAsc(ChatSession chatSession);

	long countByChatSession(ChatSession chatSession);
}
