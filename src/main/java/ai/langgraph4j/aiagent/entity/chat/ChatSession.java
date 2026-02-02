package ai.langgraph4j.aiagent.entity.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 채팅 세션 (DB 영구 저장)
 * 새 세션의 대화는 저장 버튼 없이 항상 DB에 저장됩니다.
 */
@Data
@Entity
@Table(name = "chat_session", uniqueConstraints = {
		@UniqueConstraint(name = "uk_chat_session_session_id", columnNames = "session_id")
})
@EqualsAndHashCode(of = "id")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 세션 식별자 (Redis와 동일한 값, 예: session-uuid)
	 */
	@Column(name = "session_id", nullable = false, length = 64)
	private String sessionId;

	/**
	 * 세션 제목 (첫 사용자 메시지 요약 또는 "새 대화")
	 */
	@Column(name = "title", nullable = false, length = 500)
	private String title;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Builder.Default
	@OneToMany(mappedBy = "chatSession", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ChatMessage> messages = new ArrayList<>();
}
