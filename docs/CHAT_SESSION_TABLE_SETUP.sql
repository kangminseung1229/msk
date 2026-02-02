-- ============================================
-- 채팅 세션/메시지 테이블 생성 (PostgreSQL)
-- ============================================
--
-- 새 세션의 대화는 저장 버튼 없이 항상 DB에 저장됩니다.
-- 왼쪽 탭 세션 목록 및 과거 대화 조회에 사용합니다.
--
-- 사용 전: spring.jpa.hibernate.ddl-auto=none 이므로 수동 실행 필요
-- ============================================

-- 1. 채팅 세션
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    title VARCHAR(500) NOT NULL DEFAULT '새 대화',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_chat_session_session_id UNIQUE (session_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_session_updated_at ON chat_session (updated_at DESC);

-- 2. 채팅 메시지
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGSERIAL PRIMARY KEY,
    chat_session_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    sequence_order INT NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_chat_message_session FOREIGN KEY (chat_session_id) REFERENCES chat_session (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_message_session_id ON chat_message (chat_session_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_session_order ON chat_message (chat_session_id, sequence_order);

COMMENT ON TABLE chat_session IS '채팅 세션 (Redis sessionId와 동일 값 저장)';
COMMENT ON TABLE chat_message IS '채팅 메시지 (USER/AI)';
