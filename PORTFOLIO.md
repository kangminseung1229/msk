# LangGraph4j 기반 세무 상담 AI 에이전트

> **RAG(Retrieval-Augmented Generation) 패턴을 활용한 지능형 세무 상담 시스템**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.1-blue.svg)](https://spring.io/projects/spring-ai)
[![LangGraph4j](https://img.shields.io/badge/LangGraph4j-1.7.5-orange.svg)](https://github.com/langgraph4j/langgraph4j)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue.svg)](https://www.postgresql.org/)

## 📋 프로젝트 개요

LangGraph4j와 Spring AI를 활용하여 구축한 **세무 상담 전문 AI 에이전트**입니다. RAG(Retrieval-Augmented Generation) 패턴을 통해 과거 상담 사례와 법령 조문을 벡터 검색하여 정확하고 신뢰할 수 있는 답변을 생성합니다.

### 핵심 가치

- 🎯 **도메인 특화**: 세무 상담 데이터와 법령 조문 통합 검색
- 🔍 **하이브리드 검색**: 상담 사례와 법령 조문을 동시에 검색하여 통합 결과 제공
- 🤖 **자동 도구 선택**: Spring AI가 LLM의 의도에 따라 적절한 도구를 자동으로 선택 및 실행
- 💬 **대화형 세션**: Redis 기반 세션 관리로 문맥을 유지하는 멀티 턴 대화 지원
- ⚡ **실시간 스트리밍**: SSE를 통한 실시간 응답 스트리밍

---

## 🛠️ 기술 스택

### 핵심 프레임워크
- **Spring Boot 3.5.1**: 애플리케이션 프레임워크
- **Spring AI 1.1.1**: AI 모델 통합 및 벡터 스토어
- **LangGraph4j 1.7.5**: 에이전트 워크플로우 관리
- **LangChain4j 0.34.0**: LLM 통합 라이브러리

### AI 모델
- **Chat Model**: Google Gemini 2.5 Flash (`gemini-2.5-flash`)
- **Embedding Model**: Gemini Embedding (`gemini-embedding-001`)
  - 차원: 1536
  - Task Type: RETRIEVAL_DOCUMENT

### 데이터베이스
- **PostgreSQL**: 관계형 데이터베이스
- **pgvector**: 벡터 임베딩 저장 및 유사도 검색
  - 인덱스 타입: HNSW (High-dimensional Nearest Neighbor Search)
  - 거리 측정: COSINE_DISTANCE

### 기타 기술
- **Redis**: 세션 관리 및 대화 히스토리 저장
- **QueryDSL 5.0.0**: 타입 안전한 쿼리 작성
- **EvalEx 3.6.0**: 수학 표현식 평가
- **SpringDoc OpenAPI 2.3.0**: API 문서화 (Swagger)
- **Thymeleaf**: 템플릿 엔진 (스트리밍 UI)

---

## 🎯 주요 기능

### 1. RAG 기반 지식 검색
- **벡터 유사도 검색**: 사용자 질문과 유사한 과거 상담 사례 검색
- **법령 조문 검색**: 관련 법령 조문 자동 검색
- **연관 법령 자동 검색**: 상담 결과에서 추출한 법령 정보 기반 추가 검색
- **하이브리드 검색**: 상담 데이터와 법령 데이터를 동시에 검색하여 통합 결과 제공

### 2. LangGraph 기반 워크플로우
```
사용자 요청
    ↓
[InputNode] → 사용자 입력 처리 및 상태 초기화
    ↓
[LlmNode] → LLM 호출 (Spring AI가 자동으로 Tool 선택 및 실행)
    ├─ CalculatorTool (수학 계산)
    ├─ WeatherTool (날씨 조회)
    └─ SearchTool (벡터 검색 - RAG)
    ↓
[ConditionalNode] → 다음 단계 결정
    ├─ "response" → 응답 완료
    └─ "error" → 에러 처리
    ↓
[ResponseNode] → 최종 응답 포맷팅
    ↓
사용자 응답
```

### 3. 세션 기반 대화 관리
- **Redis 세션 저장**: 대화 히스토리를 Redis에 저장하여 문맥 유지
- **자동 세션 생성**: 세션 ID가 없으면 자동으로 생성
- **멀티 턴 대화**: 이전 대화를 기억하여 연속적인 대화 지원

### 4. 실시간 스트리밍
- **SSE (Server-Sent Events)**: 실시간 응답 스트리밍
- **중간 결과 전송**: 각 단계별 진행 상황 실시간 전송
- **이벤트 타입**: `start`, `step`, `chunk`, `validation`, `complete`, `error`

### 5. 답변 검수 기능
- **ValidationNode**: LLM이 생성한 답변을 검수하여 품질 평가
- **검수 점수**: 0.0 ~ 1.0 점수로 답변 품질 평가
- **재생성 필요 여부**: 검수 실패 시 재생성 필요 여부 판단

---

## 🏗️ 아키텍처

### 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                      Client (Browser/API)                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Application                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              ChatV2Controller                         │  │
│  │  - REST API 엔드포인트                                 │  │
│  │  - SSE 스트리밍 지원                                    │  │
│  └──────────────────┬───────────────────────────────────┘  │
│                     │                                        │
│  ┌──────────────────▼───────────────────────────────────┐  │
│  │              ChatV2Service                            │  │
│  │  - 세션 관리 (Redis)                                    │  │
│  │  - 대화 히스토리 관리                                   │  │
│  └──────────────────┬───────────────────────────────────┘  │
│                     │                                        │
│  ┌──────────────────▼───────────────────────────────────┐  │
│  │              AgentGraph (LangGraph4j)                 │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐          │  │
│  │  │InputNode │→ │ LlmNode  │→ │Conditional│          │  │
│  │  └──────────┘  └────┬─────┘  │   Node   │          │  │
│  │                     │         └──────────┘          │  │
│  │                     │                                │  │
│  │              ┌──────▼──────┐                        │  │
│  │              │ Spring AI   │                        │  │
│  │              │ Tool 자동 호출│                        │  │
│  │              └──────┬──────┘                        │  │
│  │                     │                                │  │
│  │  ┌──────────────────┼──────────────────┐            │  │
│  │  │                  │                  │            │  │
│  │  ▼                  ▼                  ▼            │  │
│  │  CalculatorTool  SearchTool  WeatherTool            │  │
│  │  └──────────────┴──────────┴──────────┘            │  │
│  │                     │                                │  │
│  │              ┌──────▼──────┐                        │  │
│  │              │SearchTool   │                        │  │
│  │              │(RAG)        │                        │  │
│  │              └──────┬──────┘                        │  │
│  └─────────────────────┼───────────────────────────────┘  │
│                        │                                    │
│  ┌─────────────────────▼───────────────────────────────┐  │
│  │      ConsultationSearchService                       │  │
│  │  - 벡터 검색 (pgvector)                              │  │
│  │  - 하이브리드 검색 (상담 + 법령)                      │  │
│  └─────────────────────┬───────────────────────────────┘  │
└────────────────────────┼────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
    ┌─────────┐   ┌──────────┐   ┌──────────┐
    │PostgreSQL│   │  Redis   │   │  Gemini  │
    │+ pgvector│   │ (Session)│   │   API    │
    └─────────┘   └──────────┘   └──────────┘
```

### RAG 검색 프로세스

1. **사용자 질문 입력**: "부가가치세 신고 기한이 언제인가요?"
2. **질문 임베딩**: Gemini Embedding으로 벡터 변환
3. **하이브리드 검색**:
   - 상담 데이터 검색 (벡터 유사도, 상위 10건)
   - 법령 데이터 검색 (벡터 유사도, 상위 10건)
   - 연관 법령 검색 (상담 결과의 `lawArticlePairs` 기반)
4. **결과 통합**: 중복 제거 및 정렬
5. **컨텍스트 제공**: 검색 결과를 LLM에 컨텍스트로 제공
6. **답변 생성**: 컨텍스트를 기반으로 정확한 답변 생성

---

## 💡 기술적 하이라이트

### 1. Spring AI Tool 자동 호출
Spring AI가 LLM의 의도에 따라 적절한 도구를 자동으로 선택하고 실행합니다. 이를 통해 복잡한 워크플로우를 단순화하고 유지보수성을 향상시켰습니다.

```java
@Tool(description = "상담 데이터를 벡터 검색합니다.")
public String search(@ToolParam(description = "검색할 키워드") String query) {
    // Spring AI가 자동으로 이 메서드를 호출
    List<SearchResult> results = consultationSearchService.hybridSearch(query, 10, 10, 0.6);
    return formatSearchResults(query, results);
}
```

### 2. 하이브리드 벡터 검색
상담 데이터와 법령 데이터를 동시에 검색하여 통합 결과를 제공합니다. 이를 통해 더 풍부한 컨텍스트를 LLM에 제공할 수 있습니다.

```java
// 상담 데이터 검색
List<SearchResult> counselResults = vectorStore.similaritySearch(
    SearchRequest.query(query)
        .withFilterExpression("documentType == 'counsel'")
        .withTopK(10)
        .withSimilarityThreshold(0.6)
);

// 법령 데이터 검색
List<SearchResult> lawResults = vectorStore.similaritySearch(
    SearchRequest.query(query)
        .withFilterExpression("documentType == 'lawArticle'")
        .withTopK(10)
        .withSimilarityThreshold(0.6)
);

// 연관 법령 추가 검색
List<SearchResult> relatedLawResults = searchRelatedLaws(counselResults);
```

### 3. 세션 기반 대화 컨텍스트
Redis를 활용하여 대화 히스토리를 저장하고 관리합니다. 이를 통해 멀티 턴 대화를 지원하고 문맥을 유지할 수 있습니다.

```java
// 세션 로드
AgentState state = sessionStore.loadSession(sessionId);
if (state == null) {
    state = new AgentState();
    state.setSessionId(sessionId);
}

// 대화 히스토리에 추가
state.getMessages().add(new UserMessage(userInput));
state.getMessages().add(new AiMessage(aiResponse));

// 세션 저장
sessionStore.saveSession(sessionId, state);
```

### 4. 스트리밍 응답
SSE를 통해 실시간으로 응답을 스트리밍합니다. 이를 통해 사용자 경험을 향상시킵니다.

```java
@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter chatStreaming(@RequestBody ChatV2Request request) {
    SseEmitter emitter = new SseEmitter(30000L);
    
    // 중간 결과 전송
    emitter.send(SseEmitter.event()
        .name("step")
        .data("LLM 응답 생성 중..."));
    
    // 스트리밍 모드로 그래프 실행
    agentGraph.executeStreaming(state, userInput, emitter);
    
    return emitter;
}
```

---

## 📁 프로젝트 구조

```
src/main/java/ai/langgraph4j/aiagent/
├── agent/                    # 에이전트 핵심 로직
│   ├── graph/               # 그래프 정의 (AgentGraph)
│   ├── nodes/               # 노드 구현
│   │   ├── InputNode        # 입력 처리
│   │   ├── LlmNode          # LLM 호출
│   │   ├── ConditionalNode # 조건 분기
│   │   ├── ResponseNode     # 응답 생성
│   │   └── ValidationNode   # 답변 검수
│   └── state/               # 상태 관리 (AgentState)
│
├── config/                  # 설정 클래스
│   ├── AiConfig             # Spring AI 설정
│   ├── EmbeddingConfig      # 임베딩 설정
│   ├── VectorStoreConfig    # 벡터 스토어 설정
│   └── ToolConfig           # 도구 설정
│
├── controller/              # REST API 컨트롤러
│   ├── ChatV2Controller     # 채팅 v2 API
│   ├── AgentTestController  # 에이전트 테스트 API
│   ├── SearchController     # 검색 API
│   └── EmbeddingController  # 임베딩 API
│
├── service/                 # 비즈니스 로직
│   ├── ChatV2Service        # 채팅 v2 서비스
│   ├── AgentService         # 에이전트 서비스
│   ├── ConsultationSearchService  # 벡터 검색 서비스
│   ├── CounselEmbeddingService     # 상담 임베딩 서비스
│   ├── LawArticleEmbeddingService # 법령 임베딩 서비스
│   └── SessionStore         # 세션 저장소 (Redis)
│
├── tools/                   # AI 도구
│   ├── CalculatorTool       # 계산기
│   ├── SearchTool           # 벡터 검색 (RAG)
│   └── WeatherTool          # 날씨 조회
│
├── entity/                  # JPA 엔티티
│   ├── counsel/            # 상담 관련 엔티티
│   └── law/                # 법령 관련 엔티티
│
└── repository/              # 데이터 접근 계층
    ├── CounselRepository
    └── LawBasicInformationRepository
```

---

## 🚀 주요 성과

### 기능 완성도
- ✅ **RAG 패턴 완전 구현**: 벡터 검색을 통한 지식 기반 답변 생성
- ✅ **하이브리드 검색**: 상담 데이터와 법령 데이터 통합 검색
- ✅ **Spring AI Tool 통합**: 자동 도구 선택 및 실행
- ✅ **스트리밍 지원**: 실시간 응답 스트리밍
- ✅ **세션 관리**: Redis 기반 대화 컨텍스트 유지
- ✅ **답변 검수**: ValidationNode를 통한 답변 품질 평가

### 기술적 성과
- **벡터 검색 성능**: HNSW 인덱스를 활용한 고성능 유사도 검색
- **확장 가능한 아키텍처**: 명확한 레이어 분리와 확장 가능한 구조
- **도메인 특화**: 세무 상담 도메인에 특화된 검색 및 답변 생성

---

## 📊 API 엔드포인트

### 채팅 v2 API

#### 1. 채팅 실행 (비스트리밍)
```http
POST /api/v2/chat
Content-Type: application/json

{
  "message": "부가가치세 신고 기한이 언제인가요?",
  "sessionId": "session-12345",
  "systemInstruction": "당신은 세무 전문가입니다."
}
```

#### 2. 채팅 실행 (스트리밍)
```http
POST /api/v2/chat/stream
Content-Type: application/json

{
  "message": "부가가치세 신고 기한이 언제인가요?",
  "sessionId": "session-12345"
}
```

#### 3. 데모 페이지
```http
GET /api/v2/chat/demo
```

### 검색 API

#### 벡터 검색
```http
POST /api/search
Content-Type: application/json

{
  "query": "부가가치세 신고",
  "topK": 10,
  "similarityThreshold": 0.6
}
```

---

## 🔧 환경 설정

### 필수 환경 변수
```bash
# Gemini API 키
export GEMINI_API_KEY=your-api-key-here

# PostgreSQL 설정
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aiagent
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=password

# Redis 설정
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
```

### 애플리케이션 실행
```bash
# Maven으로 실행
./mvnw spring-boot:run

# 또는 IDE에서 실행
# AiagentApplication.java 실행
```

---

## 📈 성능 최적화

### 벡터 검색 최적화
- **HNSW 인덱스**: 고성능 유사도 검색을 위한 인덱스
- **메타데이터 필터링**: `documentType` 기반 필터링으로 검색 범위 축소
- **유사도 임계값**: 기본값 0.6으로 관련성 높은 결과만 반환

### LLM 호출 최적화
- **Gemini 2.5 Flash**: 빠른 응답 속도를 위한 모델 선택
- **스트리밍**: SSE를 통한 실시간 응답으로 체감 속도 향상
- **컨텍스트 최적화**: 검색 결과를 효율적으로 포맷팅하여 토큰 사용량 최소화

---

## 🎓 학습한 기술 및 개념

### LangGraph4j
- StateGraph를 활용한 워크플로우 관리
- 노드 기반 처리 및 조건부 분기
- 상태 관리 및 Reducer 패턴

### Spring AI
- ChatModel 통합 및 Tool 자동 호출
- Vector Store 연동 및 벡터 검색
- Embedding 모델 활용

### RAG 패턴
- 벡터 임베딩 및 유사도 검색
- 하이브리드 검색 전략
- 컨텍스트 기반 답변 생성

### 벡터 데이터베이스
- PostgreSQL pgvector 활용
- HNSW 인덱스 최적화
- 메타데이터 필터링

---

## 🔮 향후 개선 계획

### 단기 (1-2주)
- [ ] 테스트 코드 작성 (단위 테스트, 통합 테스트)
- [ ] WeatherTool 실제 API 연동
- [ ] 스트리밍 완성도 개선
- [ ] 에러 처리 강화

### 중기 (1-2개월)
- [ ] 체크포인트 기능 구현
- [ ] Observability 강화 (Tracing, 메트릭)
- [ ] API 문서화 완성
- [ ] 대화 히스토리 영구 저장

### 장기 (3-6개월)
- [ ] 병렬 처리 구현
- [ ] Vector Store 최적화
- [ ] CI/CD 파이프라인 구축
- [ ] 성능 최적화 및 벤치마크

---

## 📚 참고 자료

- [LangGraph4j 공식 문서](https://langgraph4j.github.io/langgraph4j/)
- [Spring AI 공식 문서](https://docs.spring.io/spring-ai/reference/)
- [Google Gemini API 문서](https://ai.google.dev/docs)
- [PostgreSQL pgvector 문서](https://github.com/pgvector/pgvector)

---

## 📝 라이선스

이 프로젝트는 개인 포트폴리오 목적으로 제작되었습니다.

---

**작성일**: 2025-01-27  
**버전**: 0.0.1-SNAPSHOT  
**상태**: 개발 중
