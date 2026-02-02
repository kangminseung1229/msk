package ai.langgraph4j.aiagent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.langgraph4j.aiagent.entity.chat.ChatSession;

/**
 * 채팅 세션 Repository
 * test 프로파일에서는 JPA가 비활성화되므로 로드하지 않습니다.
 */
@Repository
@Profile("!test")
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

	Optional<ChatSession> findBySessionId(String sessionId);

	/**
	 * 최신순 세션 목록 (왼쪽 탭용)
	 */
	List<ChatSession> findAllByOrderByUpdatedAtDesc(org.springframework.data.domain.Pageable pageable);
}
