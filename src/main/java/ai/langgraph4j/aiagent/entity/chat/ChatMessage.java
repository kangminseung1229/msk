package ai.langgraph4j.aiagent.entity.chat;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 (DB 영구 저장)
 */
@Data
@Entity
@Table(name = "chat_message")
@EqualsAndHashCode(of = "id")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private ChatSession chatSession;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 16)
	private MessageRole role;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	/**
	 * 대화 순서 (같은 세션 내 정렬용)
	 */
	@Column(name = "sequence_order", nullable = false)
	private int sequenceOrder;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private java.time.LocalDateTime createdAt;

	public enum MessageRole {
		USER, AI
	}
}
