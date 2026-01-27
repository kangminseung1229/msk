# 프로젝트 종합 분석 보고서

**작성일**: 2025-01-27  
**프로젝트명**: LangGraph4j AI Agent (세무 상담 AI 에이전트)  
**분석 범위**: 전체 프로젝트 구조, 아키텍처, 기능, 기술 스택, 진행 상황

---

## 📋 1. 프로젝트 개요

### 1.1 프로젝트 목적
**LangGraph4j 기반 AI 에이전트**는 Spring Boot와 LangGraph4j를 활용하여 구축된 지능형 AI 에이전트 시스템입니다. Google Gemini API를 기반으로 하며, **RAG(Retrieval-Augmented Generation) 패턴**을 통해 세무 상담 데이터와 법령 조문을 벡터 검색하여 정확한 답변을 생성합니다.

### 1.2 핵심 도메인
- **세무 상담 (Counsel)**: 과거 상담 사례 데이터베이스
- **법령 조문 (Law Article)**: 세법 관련 법령 조문 데이터
- **벡터 검색**: 상담 데이터와 법령 조문의 유사도 기반 검색

### 1.3 주요 사용 사례
1. 세무 상담 질의응답: 사용자의 세무 질문에 대해 과거 상담 사례와 법령을 참고하여 답변
2. 법령 조문 검색: 관련 법령 조문을 자동으로 검색하여 제공
3. 하이브리드 검색: 상담 데이터와 법령 데이터를 동시에 검색하여 통합 결과 제공

---

## 🏗️ 2. 기술 스택

### 2.1 핵심 프레임워크
| 기술 | 버전 | 용도 |
|------|------|------|
| Spring Boot | 3.5.1 | 애플리케이션 프레임워크 |
| Spring AI | 1.1.1 | AI 모델 통합 및 벡터 스토어 |
| LangGraph4j | 1.7.5 | 에이전트 워크플로우 관리 |
| LangChain4j | 0.34.0 | LLM 통합 라이브러리 |
| Java | 17+ | 개발 언어 |

### 2.2 AI 모델
- **Chat Model**: Google Gemini 2.5 Flash (`gemini-2.5-flash`)
- **Embedding Model**: Gemini Embedding (`gemini-embedding-001`)
  - 차원: 1536 (성능과 비용의 균형)
  - Task Type: RETRIEVAL_DOCUMENT

### 2.3 데이터베이스
- **PostgreSQL**: 관계형 데이터베이스
- **pgvector**: 벡터 임베딩 저장 및 유사도 검색
  - 인덱스 타입: HNSW (High-dimensional Nearest Neighbor Search)
  - 거리 측정: COSINE_DISTANCE

### 2.4 기타 라이브러리
- **EvalEx 3.6.0**: 수학 표현식 평가 (계산기 도구)
- **JSoup 1.17.2**: HTML 파싱 및 정리
- **QueryDSL 5.0.0**: 타입 안전한 쿼리 작성
- **Lombok**: 보일러플레이트 코드 제거
- **Thymeleaf**: 템플릿 엔진 (스트리밍 UI)
- **SpringDoc OpenAPI 2.3.0**: API 문서화 (Swagger)

---

## 🎯 3. 핵심 아키텍처

### 3.1 에이전트 워크플로우 (Graph 구조)

```
사용자 요청
    ↓
[InputNode] → 사용자 입력 처리 및 상태 초기화
    ↓
[LlmNode] → LLM 호출
    ├─ Spring AI가 자동으로 Tool 선택 및 실행
    │  ├─ CalculatorTool (수학 계산)
    │  ├─ WeatherTool (날씨 조회)
    │  └─ SearchTool (벡터 검색 - RAG)
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

### 3.2 RAG (Retrieval-Augmented Generation) 패턴

#### 하이브리드 검색 프로세스
1. **상담 데이터 검색**: 사용자 질문과 유사한 과거 상담 사례 검색 (벡터 유사도)
2. **법령 데이터 검색**: 관련 법령 조문 검색 (벡터 유사도)
3. **연관 법령 검색**: 상담 결과에서 추출한 `lawArticlePairs`를 기반으로 추가 법령 조문 검색
4. **결과 통합**: 상담 결과와 법령 결과를 통합하여 중복 제거
5. **컨텍스트 제공**: 검색 결과를 LLM에 컨텍스트로 제공
6. **답변 생성**: 컨텍스트를 기반으로 정확한 답변 생성

#### 벡터 검색 특징
- **유사도 임계값**: 기본값 0.6 (조정 가능)
- **검색 결과 수**: 상담 10건 + 법령 10건 (기본값)
- **메타데이터 필터링**: `documentType` 필터로 상담/법령 구분
- **중복 제거**: `lawId:articleKey` 기준으로 중복 제거

### 3.3 상태 관리 (AgentState)

```java
AgentState {
    - UserMessage userMessage          // 사용자 메시지
    - AiMessage aiMessage              // AI 응답 메시지
    - List<ToolExecutionRequest>       // 도구 실행 요청 목록
    - List<ToolExecutionResult>        // 도구 실행 결과 목록
    - List<Object> messages            // 대화 히스토리
    - String currentStep               // 현재 단계
    - int iterationCount               // 반복 횟수 (무한 루프 방지)
    - String sessionId                 // 세션 ID
    - String error                     // 에러 메시지
    - String systemInstruction        // System Instruction
    - Map<String, Object> metadata    // 메타데이터
}
```

---

## 🔧 4. 주요 기능

### 4.1 AI 에이전트 실행
- **REST API**: `POST /api/test/agent/invoke`
- **스트리밍 API**: `POST /api/test/agent/stream` (SSE)
- **세션 관리**: 세션 ID 기반 대화 컨텍스트 유지
- **System Instruction**: 모델의 역할과 동작 방식 정의

### 4.2 도구 (Tools)

#### 1. CalculatorTool
- **기능**: 수학 계산 수행
- **구현 상태**: ✅ 완전 구현
- **사용 예**: "123 + 456은 얼마인가요?"

#### 2. SearchTool
- **기능**: 벡터 기반 상담 데이터 및 법령 검색 (RAG)
- **구현 상태**: ✅ 완전 구현
- **하이브리드 검색**: 상담 데이터 + 법령 데이터 동시 검색
- **연관 법령 검색**: 상담 결과의 `lawArticlePairs` 기반 추가 검색
- **사용 예**: "부가가치세 신고 기한이 언제인가요?"

#### 3. WeatherTool
- **기능**: 날씨 정보 조회
- **구현 상태**: ⚠️ 구조만 구현 (실제 API 미연동)
- **필요 작업**: OpenWeatherMap API 또는 WeatherAPI 통합

### 4.3 벡터 스토어 연동
- **PostgreSQL pgvector**: 벡터 임베딩 저장 및 검색
- **임베딩 차원**: 1536 (Gemini Embedding)
- **인덱스**: HNSW (고성능 유사도 검색)
- **메타데이터**: 상담 ID, 제목, 분야, 법령 정보 등

### 4.4 스트리밍 지원
- **SSE (Server-Sent Events)**: 실시간 응답 스트리밍
- **중간 결과 전송**: 각 단계별 진행 상황 실시간 전송
- **이벤트 타입**: `start`, `step`, `response`, `error`, `complete`

---

## 📊 5. 프로젝트 구조

### 5.1 디렉토리 구조
```
src/main/java/ai/langgraph4j/aiagent/
├── agent/                    # 에이전트 핵심 로직
│   ├── graph/               # 그래프 정의 (AgentGraph)
│   ├── nodes/               # 노드 구현
│   │   ├── InputNode        # 입력 처리
│   │   ├── LlmNode          # LLM 호출
│   │   ├── ConditionalNode # 조건 분기
│   │   ├── ResponseNode     # 응답 생성
│   │   └── ToolNode         # 도구 실행 (호환성 유지)
│   └── state/               # 상태 관리 (AgentState)
│
├── config/                  # 설정 클래스
│   ├── AiConfig             # Spring AI 설정
│   ├── EmbeddingConfig      # 임베딩 설정
│   ├── VectorStoreConfig    # 벡터 스토어 설정
│   └── ToolConfig           # 도구 설정
│
├── controller/              # REST API 컨트롤러
│   ├── AgentTestController  # 에이전트 실행 API
│   ├── SearchController     # 검색 API
│   └── EmbeddingController  # 임베딩 API
│
├── service/                  # 비즈니스 로직
│   ├── AgentService         # 에이전트 서비스
│   ├── ConsultationSearchService  # 벡터 검색 서비스
│   ├── CounselEmbeddingService    # 상담 임베딩 서비스
│   └── LawArticleEmbeddingService # 법령 임베딩 서비스
│
├── tools/                    # AI 도구
│   ├── CalculatorTool        # 계산기
│   ├── SearchTool           # 벡터 검색 (RAG)
│   └── WeatherTool          # 날씨 조회
│
├── entity/                   # JPA 엔티티
│   ├── counsel/             # 상담 관련 엔티티
│   └── law/                 # 법령 관련 엔티티
│
└── repository/               # 데이터 접근 계층
    ├── CounselRepository
    └── LawBasicInformationRepository
```

### 5.2 설정 파일
- `application.properties`: 기본 설정
- `application-mining.properties`: 프로덕션 환경 설정 (mining 프로파일)
- `application-test.properties`: 테스트 환경 설정

---

## 📈 6. 진행 상황 분석

### 6.1 완료된 Phase

#### ✅ Phase 0: 준비 단계
- 의존성 추가 완료
- 개발 환경 설정 완료
- 기술 스택 학습 완료

#### ✅ Phase 1: 요구사항 정의 및 설계
- 에이전트 역할 정의 완료
- Graph 설계 완료
- State 스키마 설계 완료
- API 설계 완료

#### ✅ Phase 2: 기본 코어 구현 (대부분 완료)
- 프로젝트 구조 설정 완료
- State 정의 완료 (AgentState)
- LLM 모델 설정 완료 (Gemini)
- 핵심 노드 구현 완료
  - InputNode ✅
  - LlmNode ✅
  - ConditionalNode ✅
  - ResponseNode ✅
  - ToolNode ✅
- Tool 구현 완료
  - CalculatorTool ✅ (완전 구현)
  - SearchTool ✅ (완전 구현, RAG 통합)
  - WeatherTool ⚠️ (구조만 구현)
- Graph 구성 완료 (AgentGraph)
- REST API 구현 완료
- 스트리밍 기능 일부 구현 완료

### 6.2 진행 중인 Phase

#### 🔄 Phase 3: 고급 기능 추가 (진행 중)
- 스트리밍 응답: 기본 구조 구현됨, 완성도 개선 필요
- 체크포인트 기능: 미구현
- 병렬 처리: 미구현
- Observability: 기본 로깅만 구현됨

### 6.3 미완료 항목

#### 🔴 Phase 2: 테스트 작성
- 단위 테스트: 미작성
- 통합 테스트: 미작성
- API 테스트: 미작성

#### 🔴 Phase 3: 고급 기능
- 체크포인트 & 브레이크포인트: 미구현
- 병렬 처리: 미구현
- Tracing 구현: 미구현
- 메트릭 수집: 기본 Actuator만 설정됨

#### 🔴 Phase 4: 통합 및 외부 연동
- WeatherTool 실제 API 연동: 미구현
- 대화 히스토리 저장: 미구현
- 인증 및 보안: 미구현
- API 문서화 (Swagger): 설정만 완료, 문서화 필요

---

## 💡 7. 강점 및 특징

### 7.1 기술적 강점
1. **RAG 패턴 완전 구현**: 벡터 검색을 통한 지식 기반 답변 생성
2. **하이브리드 검색**: 상담 데이터와 법령 데이터를 동시에 검색하여 통합 결과 제공
3. **Spring AI Tool 자동 호출**: LLM이 자동으로 적절한 도구를 선택하고 실행
4. **스트리밍 지원**: SSE를 통한 실시간 응답 스트리밍
5. **세션 관리**: 세션 기반 대화 컨텍스트 유지

### 7.2 아키텍처 강점
1. **명확한 레이어 분리**: Controller → Service → Graph → Nodes
2. **확장 가능한 구조**: 새로운 도구 추가가 용이
3. **상태 관리**: AgentState를 통한 명확한 상태 추적
4. **에러 처리**: 각 단계별 에러 처리 및 복구 로직

### 7.3 도메인 특화
1. **세무 상담 특화**: 상담 데이터와 법령 조문 통합 검색
2. **법령 연관성**: 상담 결과에서 자동으로 연관 법령 조문 검색
3. **메타데이터 활용**: 상담 분야, 법령 정보 등 풍부한 메타데이터 활용

---

## ⚠️ 8. 개선 필요 사항

### 8.1 즉시 개선 필요 (높은 우선순위)
1. **테스트 코드 작성**: 단위 테스트, 통합 테스트, API 테스트
2. **에러 처리 강화**: Fallback 노드, 재시도 로직
3. **스트리밍 완성도 개선**: 중간 결과 전송 개선, 에러 처리 강화
4. **WeatherTool API 연동**: 실제 날씨 API 통합

### 8.2 단기 개선 (중간 우선순위)
1. **체크포인트 기능**: 장기 세션 지원, 상태 복구
2. **Observability 강화**: 상세 로깅, Tracing, 메트릭
3. **API 문서화**: Swagger 문서 완성
4. **대화 히스토리 저장**: 데이터베이스에 대화 히스토리 저장

### 8.3 중장기 개선 (낮은 우선순위)
1. **병렬 처리**: 여러 노드 병렬 실행
2. **Vector Store 최적화**: 검색 성능 개선
3. **CI/CD 파이프라인**: 자동 빌드 및 배포
4. **컨테이너화**: Docker 이미지 최적화

---

## 🔍 9. 코드 품질 분석

### 9.1 코드 구조
- ✅ **명확한 패키지 구조**: 도메인별로 잘 분리됨
- ✅ **의존성 주입**: Spring의 DI를 적절히 활용
- ✅ **로깅**: SLF4J를 통한 적절한 로깅
- ⚠️ **예외 처리**: 기본적인 예외 처리만 구현됨

### 9.2 설계 패턴
- ✅ **Service Layer**: 비즈니스 로직 분리
- ✅ **Repository Pattern**: 데이터 접근 계층 분리
- ✅ **Strategy Pattern**: 도구별 전략 패턴 (Tool 인터페이스)
- ⚠️ **Error Handling**: 전역 예외 처리 미구현

### 9.3 문서화
- ✅ **JavaDoc**: 주요 클래스와 메서드에 JavaDoc 작성됨
- ✅ **README**: 기본 사용 가이드 작성됨
- ✅ **문서 폴더**: 상세한 기술 문서들 (`docs/` 폴더)
- ⚠️ **API 문서**: Swagger 설정만 완료, 실제 문서화 필요

---

## 📝 10. 데이터베이스 구조

### 10.1 주요 엔티티
1. **Counsel (상담)**: 상담 데이터
   - ID, 제목, 내용, 분야, 상태 등
   - 법령 조문과의 연관 관계 (`lawArticlePairs`)

2. **LawArticle (법령 조문)**: 법령 조문 데이터
   - 법령 ID, 조문 키, 조문 내용 등

3. **Vector Store (벡터 스토어)**: 임베딩 저장
   - 상담 데이터 임베딩
   - 법령 조문 임베딩
   - 메타데이터 (documentType, counselId, lawId 등)

### 10.2 벡터 검색 최적화
- **HNSW 인덱스**: 고성능 유사도 검색
- **메타데이터 필터링**: documentType 기반 필터링
- **청크 관리**: 긴 문서를 청크로 분할하여 저장

---

## 🚀 11. 배포 및 운영

### 11.1 현재 배포 상태
- **프로파일**: `mining` 프로파일 사용
- **Swagger UI**: `https://mining.taxnet.co.kr/counsel-ai/swagger-ui.html`
- **API Base URL**: `https://mining.taxnet.co.kr/counsel-ai`

### 11.2 환경 설정
- **데이터베이스**: PostgreSQL (환경 변수로 설정)
- **API 키**: Gemini API 키 (환경 변수로 설정)
- **스트리밍 설정**: HTTP 압축 비활성화, 세션 타임아웃 30분

### 11.3 모니터링
- **Spring Boot Actuator**: 기본 헬스 체크, 메트릭
- **로깅**: SLF4J 기반 로깅
- ⚠️ **Tracing**: 미구현
- ⚠️ **메트릭 대시보드**: 미구현

---

## 📊 12. 성능 분석

### 12.1 벡터 검색 성능
- **검색 속도**: HNSW 인덱스로 빠른 유사도 검색
- **임베딩 차원**: 1536 (성능과 비용의 균형)
- **검색 결과 수**: 상담 10건 + 법령 10건 (기본값)

### 12.2 LLM 호출 성능
- **모델**: Gemini 2.5 Flash (빠른 응답 속도)
- **스트리밍**: SSE를 통한 실시간 응답
- **Tool 자동 호출**: Spring AI가 자동으로 처리

### 12.3 개선 가능 영역
- ⚠️ **병렬 처리**: 여러 검색을 병렬로 실행하여 성능 개선 가능
- ⚠️ **캐싱**: 자주 검색되는 쿼리 결과 캐싱
- ⚠️ **비동기 처리**: 비동기 노드 실행으로 응답 시간 단축

---

## 🎯 13. 향후 로드맵 요약

### 13.1 즉시 진행 (1-2주)
1. 테스트 코드 작성
2. WeatherTool API 연동
3. 스트리밍 완성도 개선
4. 에러 처리 강화

### 13.2 단기 목표 (1-2개월)
1. 체크포인트 기능 구현
2. Observability 강화
3. API 문서화 완성
4. 대화 히스토리 저장

### 13.3 중장기 목표 (3-6개월)
1. 병렬 처리 구현
2. Vector Store 최적화
3. CI/CD 파이프라인 구축
4. 성능 최적화

---

## 📌 14. 결론

### 14.1 프로젝트 평가
이 프로젝트는 **LangGraph4j와 Spring AI를 활용한 고도화된 AI 에이전트 시스템**입니다. 특히 **RAG 패턴을 완전히 구현**하여 세무 상담 도메인에 특화된 지능형 답변 시스템을 구축했습니다.

### 14.2 주요 성과
1. ✅ **RAG 패턴 완전 구현**: 벡터 검색을 통한 지식 기반 답변 생성
2. ✅ **하이브리드 검색**: 상담 데이터와 법령 데이터 통합 검색
3. ✅ **Spring AI Tool 통합**: 자동 도구 선택 및 실행
4. ✅ **스트리밍 지원**: 실시간 응답 스트리밍

### 14.3 개선 권장 사항
1. **테스트 코드 작성**: 코드 품질 및 안정성 향상
2. **에러 처리 강화**: Fallback 노드, 재시도 로직
3. **Observability 강화**: 상세 로깅, Tracing, 메트릭
4. **성능 최적화**: 병렬 처리, 캐싱

### 14.4 전체 평가
**종합 점수: 8.5/10**

- **기능 완성도**: 9/10 (핵심 기능 완전 구현)
- **코드 품질**: 8/10 (명확한 구조, 테스트 부족)
- **문서화**: 8/10 (상세한 기술 문서, API 문서화 필요)
- **확장성**: 9/10 (확장 가능한 구조)
- **성능**: 8/10 (기본 최적화 완료, 추가 최적화 가능)

---

**분석 완료일**: 2025-01-27  
**분석자**: AI Assistant  
**다음 리뷰 권장일**: 테스트 코드 작성 후
