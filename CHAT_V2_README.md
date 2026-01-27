# 채팅 v2 API 가이드

LangGraph 기반 채팅 시스템 v2는 기존의 단순 질문-답변 형식에서 벗어나 **대화형 채팅**을 지원합니다.

## 주요 개선 사항

### ✅ LangGraph 활용
- 기존 `GeminiTextController`는 LangGraph를 사용하지 않았지만, v2는 **AgentGraph를 활용**하여 워크플로우를 관리합니다.
- 노드 기반 처리: InputNode → LlmNode → ConditionalNode → ResponseNode

### ✅ 세션 기반 대화 컨텍스트
- **Redis에 세션 저장**: 대화 히스토리를 Redis에 저장하여 문맥을 이어갑니다.
- **세션 ID로 식별**: 같은 세션 ID를 사용하면 이전 대화를 기억합니다.
- **자동 세션 생성**: 세션 ID가 없으면 자동으로 생성합니다.

### ✅ 답변 검수 기능
- **ValidationNode**: LLM이 생성한 답변을 검수하여 품질을 평가합니다.
- **검수 점수**: 0.0 ~ 1.0 점수로 답변 품질 평가
- **재생성 필요 여부**: 검수 실패 시 재생성 필요 여부 판단

## 데모 페이지

웹 브라우저에서 채팅 v2를 테스트할 수 있는 데모 페이지가 제공됩니다.

**URL**: `http://localhost:8080/api/v2/chat/demo`

### 주요 기능
- 💬 실시간 채팅 UI
- 🔄 스트리밍 응답 표시
- 📊 답변 검수 결과 표시
- 🔑 세션 관리 (새 세션 생성)
- ⚙️ System Instruction 설정

### 사용 방법
1. 브라우저에서 데모 페이지 접속
2. 메시지 입력 후 전송
3. AI 응답이 실시간으로 스트리밍됩니다
4. 검수 결과가 자동으로 표시됩니다
5. 같은 세션에서 계속 대화하면 이전 대화를 기억합니다

## API 엔드포인트

### 1. 채팅 실행 (비스트리밍)

**POST** `/api/v2/chat`

```json
{
  "message": "부가가치세 신고 기한이 언제인가요?",
  "sessionId": "session-12345",  // 선택사항, 없으면 새 세션 생성
  "systemInstruction": "당신은 세무 전문가입니다.",  // 선택사항
  "model": "gemini-2.5-flash"  // 선택사항
}
```

**응답:**
```json
{
  "response": "부가가치세 신고 기한은...",
  "sessionId": "session-12345",
  "validation": {
    "score": 0.85,
    "passed": true,
    "feedback": "답변이 정확하고 명확합니다.",
    "needsRegeneration": false
  },
  "executionTime": 2.34,
  "metadata": {
    "validationScore": 0.85,
    "validationPassed": true
  }
}
```

### 2. 채팅 실행 (스트리밍)

**POST** `/api/v2/chat/stream`

요청 본문은 위와 동일합니다.

**응답 (SSE):**
```
event: start
data: 채팅을 시작합니다...

event: step
data: 입력 처리 중...

event: step
data: LLM 응답 생성 중...

event: chunk
data: 부가가치세

event: chunk
data: 신고

event: chunk
data: 기한은...

event: validation
data: {"score":0.85,"passed":true,"feedback":"답변이 정확합니다.","needsRegeneration":false}

event: complete
data: 채팅이 완료되었습니다. 실행 시간: 2.34초
```

### 3. GET 요청 (비스트리밍)

**GET** `/api/v2/chat?message=질문&sessionId=session-12345&systemInstruction=지시사항`

### 4. GET 요청 (스트리밍)

**GET** `/api/v2/chat/stream?message=질문&sessionId=session-12345&systemInstruction=지시사항`

## 사용 예제

### 예제 1: 새 세션으로 시작

```bash
curl -X POST http://localhost:8080/api/v2/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "안녕하세요"
  }'
```

응답에서 `sessionId`를 받아서 다음 요청에 사용합니다.

### 예제 2: 세션을 이어서 대화

```bash
# 첫 번째 메시지
curl -X POST http://localhost:8080/api/v2/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "제 이름은 홍길동입니다",
    "sessionId": "session-12345"
  }'

# 두 번째 메시지 (같은 세션)
curl -X POST http://localhost:8080/api/v2/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "제 이름이 뭐였죠?",
    "sessionId": "session-12345"
  }'
```

두 번째 요청에서 AI는 "홍길동"이라고 답변합니다 (이전 대화를 기억).

### 예제 3: 스트리밍 채팅

```bash
curl -X POST http://localhost:8080/api/v2/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "부가가치세 신고 기한이 언제인가요?",
    "sessionId": "session-12345"
  }'
```

## 설정

### Redis 설정

`application.properties`에서 Redis 연결 설정:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
```

환경 변수로도 설정 가능:
```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_password
```

### 답변 검수 설정

```properties
# 검수 활성화 여부
agent.validation.enabled=true

# 검수 최소 점수 (0.0 ~ 1.0)
agent.validation.min-score=0.7
```

## 아키텍처

### 워크플로우

```
사용자 요청
    ↓
[ChatV2Controller] → 세션 로드/생성
    ↓
[ChatV2Service] → AgentGraph 실행
    ↓
[AgentGraph]
    ├─ [InputNode] → 사용자 입력 처리
    ├─ [LlmNode] → LLM 호출 (대화 히스토리 포함)
    ├─ [ConditionalNode] → 다음 단계 결정
    └─ [ResponseNode] → 응답 생성
    ↓
[ValidationNode] → 답변 검수
    ↓
[SessionStore] → Redis에 세션 저장
    ↓
사용자 응답
```

### 대화 히스토리 관리

1. **세션 로드**: Redis에서 기존 세션과 대화 히스토리 조회
2. **대화 히스토리 포함**: LlmNode에서 이전 대화를 포함하여 LLM 호출
3. **히스토리 저장**: 새 메시지와 응답을 Redis에 저장

### 답변 검수 프로세스

1. **검수 프롬프트 생성**: 사용자 질문과 AI 답변을 기반으로 검수 프롬프트 생성
2. **LLM 검수**: 별도의 LLM 호출로 답변 품질 평가
3. **결과 파싱**: JSON 형식의 검수 결과 파싱
4. **메타데이터 저장**: 검수 점수, 통과 여부, 피드백 등을 메타데이터에 저장

## 기존 v1과의 차이점

| 기능 | v1 (GeminiTextController) | v2 (ChatV2Controller) |
|------|---------------------------|----------------------|
| LangGraph 활용 | ❌ 사용 안 함 | ✅ AgentGraph 활용 |
| 대화 컨텍스트 | ❌ 단일 턴만 지원 | ✅ 세션 기반 멀티 턴 대화 |
| 세션 관리 | ❌ 없음 | ✅ Redis 기반 세션 관리 |
| 답변 검수 | ❌ 없음 | ✅ ValidationNode로 검수 |
| 대화 히스토리 | ❌ 없음 | ✅ Redis에 저장/조회 |

## 문제 해결

### Redis 연결 오류
```
Could not connect to Redis
```
→ Redis 서버가 실행 중인지 확인하고, `application.properties`의 Redis 설정을 확인하세요.

### 세션을 찾을 수 없음
```
세션을 찾을 수 없음
```
→ 정상 동작입니다. 새 세션이 자동으로 생성됩니다.

### 검수 결과 파싱 실패
```
검수 결과 파싱 실패
```
→ 검수는 선택적 기능이므로 실패해도 채팅은 계속 진행됩니다. 기본값(점수 0.8, 통과)이 사용됩니다.

## 다음 단계

- [ ] 검수 실패 시 자동 재생성 기능
- [ ] 세션 만료 시간 설정
- [ ] 세션 목록 조회 API
- [ ] 대화 히스토리 삭제 API
