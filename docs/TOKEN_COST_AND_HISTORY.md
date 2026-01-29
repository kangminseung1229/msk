## 채팅 히스토리, 토큰 비용, 최적화 전략

이 문서는 `ChatV2 + LangGraph` 기반 채팅에서 **대화 히스토리 관리 방식**과  
**토큰 사용량 로깅 / 최적화 전략**을 정리한 문서입니다.

---

## 1. 전체 구조 요약

- **세션 기반 대화**
  - `sessionId` 단위로 Redis에 상태와 히스토리를 저장
  - 서비스 레벨: `ChatV2Service`
  - 세션/히스토리 저장소: `SessionStore`

- **LLM 호출**
  - 그래프 노드: `LlmNode`
  - Spring AI `ChatModel`을 통해 Gemini 호출
  - `AgentState.messages`에 담긴 히스토리 + 현재 질문을 모두 LLM에 전달

- **RAG / 내부 벡터 검색**
  - `ConsultationSearchService` + `SearchTool` + `GeminiTextService` 에서  
    벡터 검색 결과를 System Instruction 또는 컨텍스트로 LLM에 포함

---

## 2. Redis에 저장되는 형태

### 2.1. 세션 상태 (`chat:session:{sessionId}`)

- 키: `chat:session:{sessionId}`
- 값: `AgentStateDto` 를 JSON으로 직렬화한 문자열
- 주요 필드:
  - `userMessage`, `aiMessage`
  - `messages`: `List<MessageDto>` = 전체 히스토리
  - `metadata`, `systemInstruction`, `sessionId` 등

`AgentStateDto` ↔ `AgentState` 변환 시,

- `MessageDto(type="USER"|"AI", text="...")`
- → `dev.langchain4j.data.message.UserMessage` / `AiMessage` 로 복원

### 2.2. 대화 히스토리 (`chat:history:{sessionId}`)

- 키: `chat:history:{sessionId}`
- 값: `List<MessageDto>` 를 JSON으로 직렬화한 문자열
- 예시 구조:

```json
[
  { "type": "USER", "text": "첫 번째 질문" },
  { "type": "AI",   "text": "첫 번째 답변" },
  { "type": "USER", "text": "두 번째 질문" },
  { "type": "AI",   "text": "두 번째 답변" }
]
```

**중요:** Redis에는 모두 **암호화되지 않은 평문 JSON**으로 저장된다.  
→ Redis 접근 권한, 네트워크 보안, 비밀번호 관리가 중요.

---

## 3. 히스토리 로딩과 LLM 입력으로의 변환

### 3.1. 세션 로드 (`ChatV2Service.loadOrCreateSession`)

- `SessionStore.loadSession(sessionId)` 로 `AgentState` 복원
- 별도로 `SessionStore.getHistory(sessionId)` 호출해
  - `List<Object>` (실제 타입: `UserMessage` / `AiMessage`) 로 히스토리 로드
  - `state.setMessages(history)` 로 `AgentState`에 주입

### 3.2. LLM 입력 메시지 구성 (`LlmNode.prepareMessages`)

`prepareMessages(AgentState state)` 안에서:

- 1개 System 메시지
  - `state.systemInstruction` 이 있으면 그걸 사용
  - 없으면 한국어 기본 System 프롬프트 사용
- `state.messages` 히스토리 전체
  - `UserMessage` → Spring AI `UserMessage`
  - `AiMessage` → Spring AI `AssistantMessage`
- 현재 사용자 메시지 (`state.userMessage`)

최종적으로 `List<Message>` 를 만들어 `new Prompt(messages)` 로 LLM에 전달한다.

---

## 4. 토큰 폭증 문제와 해결 전략

### 4.1. 문제 인식

- 세션 단위로 **모든 히스토리를 계속 누적**해서 LLM에 전달하면:
  - 질문을 10번, 20번… 할수록 Prompt 길이가 선형으로 증가
  - → **토큰 비용이 기하급수적으로 증가**
  - → 무료/유료 구분 없이 API 할당량을 빨리 소진

### 4.2. 현재 적용한 해결책: 히스토리 슬라이딩 윈도우

`SessionStore` 에서 **히스토리를 최근 N개만 유지**하도록 구현:

- 설정 (`application.properties`)

```properties
# ============================================
# 채팅 히스토리 설정 (토큰 비용 최적화)
# ============================================
# 대화 히스토리 최대 메시지 수 (질문+답변 쌍 기준)
# 예: 20이면 최근 10개 질문+답변 쌍만 유지 (총 20개 메시지)
# 0 또는 음수로 설정하면 제한 없음 (모든 히스토리 유지)
# 권장값: 20~40 (질문 10~20개 정도)
chat.history.max-messages=20
```

- `SessionStore` 필드:

```java
@Value("${chat.history.max-messages:20}")
private int maxHistoryMessages;
```

- 히스토리 추가 시 (`addToHistory`):
  - 기존 히스토리 로드 → 새 `UserMessage` / `AiMessage` 추가
  - `limitHistorySize(history)` 로 **앞부분을 잘라내고 최근 N개만 유지**
  - 다시 Redis에 저장

- 히스토리 조회 시 (`getHistoryAsDtos`):
  - Redis에서 `List<MessageDto>` 로 로드
  - 다시 `limitHistorySize(...)` 적용 (이중 방어)

→ 결과:

- 세션 내 질문을 100번 해도
  - Redis / LLM에는 **최근 N개 메시지(예: 20개)** 만 포함
  - 토큰 비용은 **어느 정도 상한에서 유지**

---

## 5. LLM 토큰 사용량 로깅

### 5.1. 입력 토큰 추정 (LLM 호출 전)

`LlmNode` 에서 Prompt 생성 후, `estimateTokens(prompt)` 로 **대략적인 입력 토큰 수**를 추정:

- 방식:
  - `Prompt` / `Message` 객체를 `toString()` 으로 문자열화
  - 전체 문자 수를 기반으로
    - 한글/혼합 텍스트 기준 **1 토큰 ≈ 2.5자** 로 환산
  - 매우 정밀하진 않지만, **대략적인 비용 감지용**으로 충분

로그 예:

```text
LlmNode: LLM 호출 전 토큰 추정 - 입력 토큰: 4477개 (메시지 수: 23개)
```

### 5.2. 실제 토큰 사용량 로깅 (LLM 호출 후)

Spring AI의 `ChatResponse.getMetadata().getUsage()` 를 통해  
모델이 반환한 실제 토큰 사용량을 읽어서 로깅:

- `promptTokens` (입력 토큰)
- `totalTokens` (전체 토큰)
- `completionTokens = totalTokens - promptTokens` (출력 토큰, 직접 계산)

로그 예:

```text
LlmNode: LLM 토큰 사용량 - 입력: 5515개, 출력: 74개, 총: 5589개 (실제 사용량)
```

또한, 추정값과 실제값의 차이도 디버그 로그로 남김:

```text
LlmNode: 토큰 추정 정확도 - 추정: 4477개, 실제: 5515개, 차이: 1038개 (23.2%)
```

※ 이 정확도는 **“대략적인 모니터링용”**이며,  
정밀 과금 계산은 **반드시 실제 Usage 기준**으로 해야 한다.

---

## 6. 운영 시 튜닝 포인트

- **`chat.history.max-messages`**
  - 10~20: 토큰 비용 최소화, 컨텍스트 짧음 (최근 질문 위주)
  - 20~40: 적당한 균형 (현재 기본값: 20)
  - 0 또는 음수: 모든 히스토리 유지 (연구/디버그용, 비용 폭증 주의)

- **로그 레벨**
  - 토큰 사용량 로깅은 `INFO` / 정확도 비교는 `DEBUG`
  - 운영 환경에서는 `INFO`만 보고 싶으면 `DEBUG` 로그 레벨만 낮추면 됨

- **보안**
  - Redis에 평문으로 질문/답변이 저장되므로
    - Redis 접근 제어, 암호, 네트워크 보안 필수
    - 특히 민감 데이터(개인정보 등)를 질문으로 받는 경우 주의

---

## 7. 요약

- **히스토리 관리**
  - Redis에 세션/히스토리 평문 JSON 저장
  - `chat.history.max-messages` 로 **최근 N개만 유지 (슬라이딩 윈도우)**

- **토큰 비용 관리**
  - LLM 호출 전: Prompt 기반 토큰 수 **대략 추정**
  - LLM 호출 후: Usage 메타데이터로 **실제 토큰 사용량 수집**
  - 로그로 입력/출력/총 토큰을 확인하면서 비용 트렌드 모니터링

- 이 구조 덕분에:
  - **멀티턴 대화의 문맥 유지**와
  - **토큰 비용 통제**를 동시에 달성할 수 있다.

