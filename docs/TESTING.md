# Phase 2 테스트 가이드

## 테스트 컨트롤러

Phase 2 테스트를 위한 `AgentTestController`가 구현되었습니다.

### 엔드포인트

#### 1. POST /api/test/agent/invoke
에이전트를 실행하여 응답을 받습니다.

**요청 예시**:
```bash
curl -X POST http://localhost:8080/api/test/agent/invoke \
  -H "Content-Type: application/json" \
  -d '{
    "message": "안녕하세요",
    "sessionId": "test-session-123"
  }'
```

**응답 예시**:
```json
{
  "response": "안녕하세요! 무엇을 도와드릴까요?",
  "sessionId": "test-session-123",
  "toolsUsed": [],
  "executionTime": 1.5,
  "iterationCount": 1
}
```

#### 2. GET /api/test/agent/test?message=안녕하세요
간단한 GET 요청으로 테스트합니다.

**요청 예시**:
```bash
curl http://localhost:8080/api/test/agent/test?message=안녕하세요
```

#### 3. GET /api/test/agent/health
헬스 체크 엔드포인트입니다.

**요청 예시**:
```bash
curl http://localhost:8080/api/test/agent/health
```

## OpenAI 키 설정

현재 `application.properties`에 OpenAI 키가 설정되어 있습니다.

**⚠️ 보안 주의사항**:
- 프로덕션 환경에서는 환경 변수로 관리하세요
- `application.properties`에 키를 직접 작성하지 마세요
- Git에 커밋하기 전에 키를 제거하세요

### 환경 변수 사용 방법

1. 환경 변수 설정:
```bash
export OPENAI_API_KEY=your-api-key-here
```

2. `application.properties` 수정:
```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
```

## 테스트 시나리오

### 시나리오 1: 간단한 인사
```bash
curl -X POST http://localhost:8080/api/test/agent/invoke \
  -H "Content-Type: application/json" \
  -d '{"message": "안녕하세요"}'
```

### 시나리오 2: 질문하기
```bash
curl -X POST http://localhost:8080/api/test/agent/invoke \
  -H "Content-Type: application/json" \
  -d '{"message": "오늘 날씨가 어때?"}'
```

### 시나리오 3: 계산 요청 (Phase 3에서 도구 사용)
```bash
curl -X POST http://localhost:8080/api/test/agent/invoke \
  -H "Content-Type: application/json" \
  -d '{"message": "123 + 456은 얼마인가요?"}'
```

## 애플리케이션 실행

1. 애플리케이션 실행:
```bash
./mvnw spring-boot:run
```

2. 기본 포트: `8080`

3. 로그 확인:
- 각 노드의 실행 로그가 출력됩니다
- 에러 발생 시 상세한 스택 트레이스가 출력됩니다

## 예상 동작 흐름

1. **InputNode**: 사용자 입력 처리
2. **LlmNode**: OpenAI API 호출
3. **ConditionalNode**: 다음 단계 결정
4. **ResponseNode**: 응답 생성 및 반환

## 문제 해결

### OpenAI API 키 오류
- `application.properties`의 키가 유효한지 확인
- 환경 변수 `OPENAI_API_KEY`가 설정되어 있는지 확인

### 네트워크 오류
- 인터넷 연결 확인
- OpenAI API 접근 가능 여부 확인

### 컴파일 오류
- `./mvnw clean compile` 실행
- IDE에서 프로젝트 새로고침

---

**작성일**: 2025-01-XX
**Phase**: 2
