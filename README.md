# Gemini 무료 버전 AI 에이전트

Spring Boot와 Spring AI를 사용하여 Gemini 무료 버전으로 만든 간단한 AI 에이전트입니다.

## 🚀 빠른 시작

### 1. Gemini API 키 발급

1. [Google AI Studio](https://makersuite.google.com/app/apikey)에 접속
2. "Create API Key" 클릭하여 API 키 생성
3. 생성된 API 키를 복사

### 2. 환경 변수 설정

터미널에서 다음 명령어 실행:

```bash
export GEMINI_API_KEY=your-api-key-here
```

또는 `application.properties` 파일에서 직접 설정 (개발 환경용):

```properties
spring.ai.google.genai.api-key=your-api-key-here
```

### 3. 애플리케이션 실행

```bash
./mvnw spring-boot:run
```

또는 IDE에서 `AiagentApplication` 클래스를 실행

### 4. API 테스트

애플리케이션이 실행되면 다음 엔드포인트로 테스트할 수 있습니다:

#### GET 요청 (간단한 테스트)
```bash
curl "http://localhost:8080/api/test/agent/test?message=안녕하세요"
```

#### POST 요청 (JSON)
```bash
curl -X POST http://localhost:8080/api/test/agent/invoke \
  -H "Content-Type: application/json" \
  -d '{
    "message": "안녕하세요! 오늘 날씨가 어때요?",
    "sessionId": "test-session-1"
  }'
```

## 📝 API 엔드포인트

### 1. 간단한 테스트 (GET)
```
GET /api/test/agent/test?message={메시지}
```

### 2. 에이전트 실행 (POST)
```
POST /api/test/agent/invoke
Content-Type: application/json

{
  "message": "사용자 메시지",
  "sessionId": "세션 ID (선택사항)"
}
```

### 3. 헬스 체크
```
GET /api/test/agent/health
```

## 🔧 설정

`application.properties`에서 다음 설정을 변경할 수 있습니다:

```properties
# Gemini 모델 설정 (무료 버전: gemini-1.5-flash)
spring.ai.google.genai.chat.options.model=gemini-1.5-flash

# Temperature 설정 (0.0 ~ 1.0)
spring.ai.google.genai.chat.options.temperature=0.7

# 최대 반복 횟수
agent.max-iterations=5
```

## 💡 사용 예제

### 예제 1: 간단한 질문
```bash
curl "http://localhost:8080/api/test/agent/test?message=파이썬이란 무엇인가요?"
```

### 예제 2: 대화형 세션
```bash
# 첫 번째 메시지
curl -X POST http://localhost:8080/api/test/agent/invoke \
  -H "Content-Type: application/json" \
  -d '{"message": "안녕하세요", "sessionId": "session-1"}'

# 두 번째 메시지 (같은 세션)
curl -X POST http://localhost:8080/api/test/agent/invoke \
  -H "Content-Type: application/json" \
  -d '{"message": "제 이름은 홍길동입니다", "sessionId": "session-1"}'
```

## 📦 프로젝트 구조

```
src/main/java/ai/langgraph4j/aiagent/
├── agent/
│   ├── graph/          # 에이전트 그래프 정의
│   ├── nodes/          # 노드 구현 (Input, LLM, Conditional, Response)
│   └── state/          # 에이전트 상태 관리
├── config/             # Spring AI 설정
├── controller/         # REST API 컨트롤러
└── AiagentApplication.java # 메인 애플리케이션
```

## ⚠️ 주의사항

1. **API 키 보안**: 프로덕션 환경에서는 반드시 환경 변수로 API 키를 관리하세요.
2. **무료 티어 제한**: Gemini 무료 버전은 일일 요청 수 제한이 있습니다.
3. **모델 선택**: 무료 버전에서는 `gemini-1.5-flash` 모델을 사용하는 것이 좋습니다.

## 🐛 문제 해결

### API 키 오류
```
Google GenAI API key is not configured
```
→ 환경 변수 `GEMINI_API_KEY`를 설정했는지 확인하세요.

### 할당량 초과 오류
```
Gemini API 할당량이 초과되었습니다
```
→ Google AI Studio에서 API 사용량을 확인하거나, 다음 날까지 기다리세요.

## 📚 참고 자료

- [Spring AI 문서](https://docs.spring.io/spring-ai/reference/)
- [Google Gemini API 문서](https://ai.google.dev/docs)
- [LangGraph4j 문서](https://github.com/bsctech/langgraph4j)
