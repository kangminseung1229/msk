# 프로젝트 개요

## 📋 프로젝트 소개

**AIAgent (LangGraph4j 기반 AI 에이전트)**는 Spring Boot와 LangGraph4j를 활용하여 구축된 지능형 AI 에이전트 시스템입니다. Google Gemini API를 기반으로 하며, RAG(Retrieval-Augmented Generation) 패턴을 통해 상담 데이터를 벡터 검색하여 정확한 답변을 생성합니다.

### 주요 특징

- 🤖 **LangGraph4j 기반 에이전트**: 복잡한 워크플로우를 그래프 구조로 관리
- 🔍 **RAG 패턴**: 벡터 검색을 통한 지식 기반 답변 생성
- 🛠️ **도구 통합**: 계산기, 날씨 조회, 벡터 검색 등 다양한 도구 지원
- 📊 **벡터 스토어**: PostgreSQL pgvector를 활용한 임베딩 저장 및 검색
- 🌊 **스트리밍 지원**: SSE(Server-Sent Events)를 통한 실시간 응답 스트리밍
- 💬 **대화 세션 관리**: 세션 기반 대화 컨텍스트 유지

---

## 🛠️ 기술 스택

### 핵심 프레임워크

- **Spring Boot 3.5.1**: 애플리케이션 프레임워크
- **Spring AI 1.1.1**: AI 모델 통합 및 벡터 스토어
- **LangGraph4j 1.7.5**: 에이전트 워크플로우 관리
- **LangChain4j 0.34.0**: LLM 통합 라이브러리

### AI 모델

- **Google Gemini API**:
  - Chat Model: `gemini-3-flash-preview` (기본값)
  - Embedding Model: `text-embedding-004`
  - Thinking Level: HIGH (Gemini 3 모델 지원)

### 데이터베이스

- **PostgreSQL**: 관계형 데이터베이스
- **pgvector**: 벡터 임베딩 저장 및 유사도 검색

### 기타 라이브러리

- **EvalEx 3.6.0**: 수학 표현식 평가 (계산기 도구)
- **JSoup 1.17.2**: HTML 파싱 및 정리
- **Lombok**: 보일러플레이트 코드 제거
- **Thymeleaf**: 템플릿 엔진 (스트리밍 UI)

---

## 🏗️ 프로젝트 구조

```
src/main/java/ai/langgraph4j/aiagent/
├── AiagentApplication.java              # Spring Boot 메인 애플리케이션
│
├── config/                          # 설정 클래스
│   ├── AiConfig.java                # Spring AI 설정 (ChatModel, ChatClient)
│   ├── EmbeddingConfig.java        # 임베딩 모델 설정
│   ├── ToolConfig.java              # 도구 등록 설정
│   └── VectorStoreConfig.java      # 벡터 스토어 설정
│
├── agent/                           # AI 에이전트 핵심 로직
│   ├── state/
│   │   └── AgentState.java         # 에이전트 상태 스키마
│   ├── nodes/                       # 그래프 노드 구현
│   │   ├── InputNode.java          # 사용자 입력 처리
│   │   ├── LlmNode.java            # LLM 호출 및 응답 생성
│   │   ├── ConditionalNode.java   # 조건부 라우팅
│   │   ├── ResponseNode.java       # 최종 응답 생성
│   │   └── ToolNode.java           # 도구 실행 (호환성 유지)
│   └── graph/
│       ├── AgentGraph.java         # 에이전트 그래프 정의
│       └── GraphConfig.java        # 그래프 설정
│
├── controller/                      # REST API 컨트롤러
│   ├── AgentTestController.java    # 에이전트 테스트 API
│   ├── GeminiTextController.java   # Gemini 텍스트 생성 API
│   ├── SearchController.java       # 벡터 검색 API
│   ├── EmbeddingController.java    # 임베딩 생성 API
│   └── dto/                        # 요청/응답 DTO
│
├── service/                         # 비즈니스 로직
│   ├── AgentService.java           # 에이전트 실행 서비스
│   ├── GeminiTextService.java      # Gemini 텍스트 생성 서비스
│   ├── ConsultationSearchService.java  # 벡터 검색 서비스
│   └── ConsultationEmbeddingService.java # 임베딩 생성 서비스
│
├── tools/                           # AI 에이전트 도구
│   ├── CalculatorTool.java         # 계산기 도구
│   ├── WeatherTool.java            # 날씨 조회 도구
│   └── SearchTool.java             # 벡터 검색 도구 (RAG)
│
├── entity/                          # JPA 엔티티
│   ├── counsel/                    # 상담 관련 엔티티
│   │   ├── Counsel.java
│   │   ├── Counselor.java
│   │   └── ...
│   └── law/                        # 법령 관련 엔티티
│
└── repository/                      # 데이터 접근 계층
    └── CounselRepository.java
```

---

## 🔄 핵심 아키텍처

### 에이전트 워크플로우

```
사용자 요청
    ↓
[InputNode] → 사용자 입력 처리 및 상태 초기화
    ↓
[LlmNode] → LLM 호출
    ├─ Spring AI가 자동으로 Tool 선택 및 실행
    └─ Tool 실행 결과를 LLM에 전달하여 최종 응답 생성
    ↓
[ConditionalNode] → 다음 단계 결정
    ├─ "response" → 응답 완료
    └─ "error" → 에러 처리
    ↓
[ResponseNode] → 최종 응답 포맷팅
    ↓
사용자 응답
```

### RAG (Retrieval-Augmented Generation) 패턴

1. **임베딩 생성**: 상담 데이터를 벡터 임베딩으로 변환
2. **벡터 저장**: PostgreSQL pgvector에 임베딩 저장
3. **유사도 검색**: 사용자 질문과 유사한 상담 데이터 검색
4. **컨텍스트 제공**: 검색 결과를 LLM에 컨텍스트로 제공
5. **답변 생성**: 컨텍스트를 기반으로 정확한 답변 생성

---

## 🎯 주요 기능

### 1. AI 에이전트 실행

- 사용자 메시지를 받아 에이전트 그래프 실행
- 세션 기반 대화 컨텍스트 유지
- System Instruction 지원

### 2. 도구 자동 호출

- **계산기 도구**: 수학 계산 수행
- **날씨 도구**: 날씨 정보 조회
- **검색 도구**: 벡터 기반 상담 데이터 검색 (RAG)

### 3. 벡터 검색 (RAG)

- 상담 데이터를 벡터 임베딩으로 변환
- 유사도 기반 검색으로 관련 상담 사례 찾기
- 검색 결과를 LLM 컨텍스트로 활용

### 4. 스트리밍 응답

- SSE(Server-Sent Events)를 통한 실시간 응답 스트리밍
- 중간 단계별 진행 상황 전송
- 웹 UI를 통한 스트리밍 테스트 지원

### 5. 임베딩 관리

- 상담 데이터 일괄 임베딩 생성
- 청크 단위로 분할하여 임베딩
- 벡터 스토어에 저장 및 인덱싱

---

## 📡 API 엔드포인트

### 에이전트 API

#### 1. 에이전트 실행 (POST)

```
POST /api/test/agent/invoke
Content-Type: application/json

{
  "message": "사용자 메시지",
  "sessionId": "세션 ID (선택사항)",
  "systemInstruction": "시스템 지시사항 (선택사항)"
}
```

#### 2. 에이전트 스트리밍 (POST)

```
POST /api/test/agent/stream
Content-Type: application/json

{
  "message": "사용자 메시지",
  "sessionId": "세션 ID",
  "systemInstruction": "시스템 지시사항"
}
```

#### 3. 간단한 테스트 (GET)

```
GET /api/test/agent/test?message={메시지}
```

### Gemini 텍스트 생성 API

#### 1. 텍스트 생성 (POST)

```
POST /api/gemini/generate
Content-Type: application/json

{
  "prompt": "프롬프트",
  "model": "gemini-3-flash-preview",
  "includeThoughts": false
}
```

#### 2. 스트리밍 (POST)

```
POST /api/gemini/streaming-sse
Content-Type: application/json

{
  "systemInstruction": "시스템 지시사항",
  "userPrompt": "사용자 프롬프트",
  "model": "gemini-3-flash-preview"
}
```

### 벡터 검색 API

#### 1. 벡터 검색 (POST)

```
POST /api/search/vector
Content-Type: application/json

{
  "query": "검색어",
  "topK": 5,
  "similarityThreshold": 0.6
}
```

### 임베딩 API

#### 1. 임베딩 생성 (POST)

```
POST /api/embedding/generate
Content-Type: application/json

{
  "counselIds": [1, 2, 3]
}
```

---

## ⚙️ 설정

### 환경 변수

```bash
# Google Gemini API 키
export GEMINI_API_KEY=your-api-key-here

# PostgreSQL 데이터베이스 설정
export DB_URL=jdbc:postgresql://localhost:5432/consultation_db
export DB_USER=postgres
export DB_PASSWORD=postgres
```

### application.properties 주요 설정

```properties
# Gemini 모델 설정
spring.ai.google.genai.chat.options.model=gemini-3-flash-preview
spring.ai.google.genai.chat.options.temperature=0.7

# Embedding 모델 설정
spring.ai.google.genai.embedding.text.options.model=text-embedding-004
spring.ai.google.genai.embedding.text.options.task-type=RETRIEVAL_DOCUMENT

# 벡터 스토어 설정
spring.ai.vectorstore.pgvector.index-type=HNSW
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.dimensions=768

# 에이전트 설정
agent.max-iterations=5
```

---

## 🚀 실행 방법

### 1. 사전 요구사항

- Java 17 이상
- PostgreSQL 12 이상 (pgvector 확장 필요)
- Google Gemini API 키

### 2. 데이터베이스 설정

```sql
-- PostgreSQL에서 pgvector 확장 설치
CREATE EXTENSION IF NOT EXISTS vector;

-- 벡터 스토어 테이블 생성 (Spring AI가 자동 생성)
```

### 3. 애플리케이션 실행

```bash
# Maven으로 실행
./mvnw spring-boot:run

# 또는 IDE에서 AiagentApplication 실행
```

### 4. API 테스트

```bash
# 간단한 테스트
curl "http://localhost:8080/api/test/agent/test?message=안녕하세요"

# 에이전트 실행
curl -X POST http://localhost:8080/api/test/agent/invoke \
  -H "Content-Type: application/json" \
  -d '{
    "message": "123 + 456 계산해줘",
    "sessionId": "test-session-1"
  }'
```

---

## 📊 데이터 모델

### Counsel (상담) 엔티티

- 상담 제목, 내용, 답변
- 상담 분야 (대/중/소분류)
- 상담위원 정보
- 법령 코드 연관
- 생성일시, 수정일시

### 벡터 스토어 구조

- **Document**: 상담 데이터 청크
- **Metadata**:
  - `counselId`: 상담 ID
  - `title`: 상담 제목
  - `fieldLarge`: 대분류
  - `chunkIndex`: 청크 인덱스
  - `totalChunks`: 전체 청크 수
- **Embedding**: 768차원 벡터 (text-embedding-004)

---

## 🔧 개발 단계 (Phase)

### Phase 0: 기본 설정 ✅

- Spring Boot 프로젝트 초기화
- Spring AI 통합
- 기본 API 엔드포인트

### Phase 1: LangGraph4j 통합 ✅

- 에이전트 그래프 구조 설계
- 기본 노드 구현
- 상태 관리 시스템

### Phase 2: 도구 구현 ✅

- 계산기, 날씨, 검색 도구 구현
- Spring AI Tool 자동 호출 통합

### Phase 3: RAG 패턴 구현 ✅

- 벡터 스토어 설정
- 임베딩 생성 서비스
- 벡터 검색 서비스
- 검색 도구 통합

### Phase 4: 스트리밍 지원 ✅

- SSE 기반 스트리밍 구현
- 웹 UI 추가
- 실시간 응답 전송

---

## 📚 참고 문서

- [프로젝트 구조 상세](./PROJECT_STRUCTURE.md)
- [API 설계 문서](./docs/API_DESIGN.md)
- [그래프 설계 문서](./docs/GRAPH_DESIGN.md)
- [RAG 로드맵](./RAG_ROADMAP.md)

---

## 🔗 관련 링크

- [Spring AI 문서](https://docs.spring.io/spring-ai/reference/)
- [Google Gemini API 문서](https://ai.google.dev/docs)
- [LangGraph4j GitHub](https://github.com/bsctech/langgraph4j)
- [PostgreSQL pgvector 문서](https://github.com/pgvector/pgvector)

---

**마지막 업데이트**: 2025-01-XX
