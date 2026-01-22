# RAG 기반 상담 에이전트 로드맵

**목표**: 내부 상담 RDB를 이용한 RAG 검색을 우선 수행하여 정확한 답변 생성

**작성일**: 2025-01-XX

---

## 📋 프로젝트 개요

### 목표
- 내부 상담 RDB에서 관련 정보를 RAG로 검색
- 검색된 정보를 우선적으로 활용하여 답변 생성
- 기존 Tool 시스템과 통합하여 확장 가능한 구조 유지

### 핵심 요구사항
1. **RDB 연결**: 내부 상담 데이터베이스 연결
2. **Vector Store**: 임베딩 저장 및 검색
3. **RAG 검색 Tool**: 상담 데이터 검색 도구
4. **우선순위 로직**: RAG 검색을 먼저 수행하도록 그래프 수정
5. **데이터 파이프라인**: RDB → 임베딩 → Vector Store

---

## 🗺️ 단계별 로드맵

### Phase 1: RDB 연결 및 데이터 모델 설계 (1주)

#### 1.1 의존성 추가
- [ ] **Spring Data JPA 추가**
  ```gradle
  implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
  ```
- [ ] **데이터베이스 드라이버 추가**
  - PostgreSQL: `org.postgresql:postgresql`
  - MySQL: `com.mysql:mysql-connector-j`
  - H2 (테스트용): `com.h2database:h2`
- [ ] **Vector Store 의존성 추가**
  ```gradle
  // PostgreSQL pgvector 사용 시
  implementation 'org.springframework.ai:spring-ai-pgvector-store'
  // 또는 SimpleVectorStore (메모리 기반, 개발용)
  implementation 'org.springframework.ai:spring-ai-simple-vector-store'
  ```

#### 1.2 데이터베이스 연결 설정
- [ ] **application.properties 설정**
  ```properties
  # 데이터베이스 연결
  spring.datasource.url=jdbc:postgresql://localhost:5432/consultation_db
  spring.datasource.username=your_username
  spring.datasource.password=your_password
  spring.datasource.driver-class-name=org.postgresql.Driver
  
  # JPA 설정
  spring.jpa.hibernate.ddl-auto=validate
  spring.jpa.show-sql=true
  spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
  ```

#### 1.3 상담 데이터 모델 설계
- [ ] **상담 엔티티 설계**
  ```java
  @Entity
  @Table(name = "consultations")
  public class Consultation {
      @Id
      private Long id;
      private String title;           // 상담 제목
      private String content;         // 상담 내용
      private String category;        // 카테고리
      private String answer;          // 답변 내용
      private LocalDateTime createdAt;
      // ... 기타 필드
  }
  ```
- [ ] **Repository 인터페이스 생성**
  ```java
  public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
      // 검색 쿼리 메서드
  }
  ```

#### 1.4 데이터 모델 검증
- [ ] 실제 RDB 스키마 확인
- [ ] 필요한 필드 매핑
- [ ] 관계 설정 (FK 등)

---

### Phase 2: Vector Store 설정 및 임베딩 (1주)

#### 2.1 Vector Store 설정
- [ ] **Vector Store Bean 생성**
  ```java
  @Bean
  public VectorStore vectorStore(EmbeddingModel embeddingModel) {
      // PostgreSQL pgvector 사용
      return new PgVectorStore.Builder()
          .withEmbeddingModel(embeddingModel)
          .withDataSource(dataSource)
          .withIndexType(PgVectorStore.PgIndexType.HNSW)
          .withDistanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
          .build();
      
      // 또는 SimpleVectorStore (개발/테스트용)
      // return new SimpleVectorStore(embeddingModel);
  }
  ```
- [ ] **Embedding Model 설정**
  ```java
  @Bean
  public EmbeddingModel embeddingModel() {
      // Gemini Embedding 사용
      return new GoogleGenAiEmbeddingModel(genAiClient);
  }
  ```

#### 2.2 데이터 임베딩 파이프라인
- [ ] **임베딩 서비스 구현**
  ```java
  @Service
  public class ConsultationEmbeddingService {
      // RDB에서 상담 데이터 조회
      // 텍스트 청킹 (Chunking)
      // 임베딩 생성
      // Vector Store에 저장
  }
  ```
- [ ] **청킹 전략 설계**
  - 상담 제목 + 내용 조합
  - 최대 청크 크기 설정 (예: 1000자)
  - 오버랩 설정 (예: 200자)
- [ ] **메타데이터 관리**
  - 원본 상담 ID
  - 카테고리 정보
  - 생성 날짜 등

#### 2.3 초기 데이터 로딩
- [ ] **배치 작업 구현**
  - 기존 상담 데이터 일괄 임베딩
  - 증분 업데이트 로직
- [ ] **데이터 검증**
  - 임베딩 품질 확인
  - 검색 테스트

---

### Phase 3: RAG 검색 Tool 구현 (1주)

#### 3.1 RAG 검색 Tool 생성
- [ ] **ConsultationRagTool 구현**
  ```java
  @Component
  public class ConsultationRagTool {
      
      @Tool(description = "내부 상담 데이터베이스에서 관련 상담 내용을 검색합니다. " +
            "사용자의 질문과 관련된 과거 상담 사례와 답변을 찾아 반환합니다. " +
            "이 도구는 항상 먼저 사용되어야 합니다.")
      public String searchConsultation(
          @ToolParam(description = "검색할 질문 또는 키워드") String query) {
          // 1. 사용자 질문을 임베딩으로 변환
          // 2. Vector Store에서 유사도 검색
          // 3. 상위 N개 결과 반환
          // 4. 원본 상담 데이터 조회
          // 5. 포맷팅하여 반환
      }
  }
  ```
- [ ] **검색 파라미터 설정**
  - 최대 결과 수 (예: 5개)
  - 유사도 임계값 (예: 0.7)
  - 메타데이터 필터링 (카테고리 등)

#### 3.2 검색 결과 포맷팅
- [ ] **결과 구조화**
  ```java
  public class ConsultationSearchResult {
      private String title;
      private String content;
      private String answer;
      private Double similarity;
      private String category;
  }
  ```
- [ ] **LLM에 전달할 형식**
  - 컨텍스트로 포함할 정보 선택
  - 참조 정보 포함 (출처 명시)

#### 3.3 Tool 등록
- [ ] **ToolConfig에 추가**
  - ConsultationRagTool을 ToolCallback으로 등록
  - 우선순위 설정 (다른 Tool보다 먼저 호출되도록)

---

### Phase 4: 그래프 수정 - RAG 우선 검색 (1주)

#### 4.1 새로운 노드 추가
- [ ] **RagSearchNode 구현**
  ```java
  @Component
  public class RagSearchNode {
      public AgentState process(AgentState state) {
          // 사용자 입력을 기반으로 RAG 검색 수행
          // 검색 결과를 state에 저장
          return state;
      }
  }
  ```
- [ ] **검색 결과를 State에 저장**
  - AgentState에 `ragContext` 필드 추가
  - 검색된 상담 정보 저장

#### 4.2 그래프 흐름 수정
- [ ] **AgentGraph 수정**
  ```
  기존 흐름:
  InputNode → LlmNode → ConditionalNode → ResponseNode
  
  새로운 흐름:
  InputNode → RagSearchNode → LlmNode → ConditionalNode → ResponseNode
  ```
- [ ] **LlmNode 수정**
  - RAG 검색 결과를 SystemMessage 또는 컨텍스트로 포함
  - 프롬프트에 검색된 정보 우선 사용 지시

#### 4.3 우선순위 로직 구현
- [ ] **조건부 검색**
  - 특정 키워드 감지 시 RAG 검색 수행
  - 또는 항상 RAG 검색 수행
- [ ] **Fallback 로직**
  - RAG 검색 결과가 없을 경우 일반 LLM 응답
  - 검색 결과 신뢰도가 낮을 경우 추가 검색

---

### Phase 5: 프롬프트 엔지니어링 (3일)

#### 5.1 System Prompt 설계
- [ ] **RAG 컨텍스트 활용 지시**
  ```
  당신은 내부 상담 데이터베이스를 기반으로 답변하는 AI 어시스턴트입니다.
  
  다음 상담 사례들을 참고하여 답변하세요:
  [RAG 검색 결과]
  
  답변 시:
  1. 검색된 상담 사례를 우선적으로 참고하세요
  2. 검색된 정보가 없거나 부족한 경우에만 일반 지식으로 답변하세요
  3. 답변의 출처를 명시하세요 (예: "상담 사례 #123 참고")
  ```
- [ ] **Few-shot 예시 포함**
  - 검색 결과 활용 예시
  - 답변 형식 가이드

#### 5.2 동적 프롬프트 생성
- [ ] **컨텍스트 선택 로직**
  - 검색 결과 중 가장 관련성 높은 것 선택
  - 토큰 제한 내에서 최대 정보 포함
- [ ] **프롬프트 최적화**
  - 불필요한 정보 제거
  - 핵심 정보 강조

---

### Phase 6: 통합 및 테스트 (1주)

#### 6.1 통합 테스트
- [ ] **전체 플로우 테스트**
  - RDB 연결 → 검색 → LLM 응답
  - 다양한 질문 시나리오 테스트
- [ ] **성능 테스트**
  - 검색 속도 측정
  - LLM 응답 시간 측정
  - 전체 응답 시간 목표: 5초 이내

#### 6.2 정확도 검증
- [ ] **답변 품질 평가**
  - 검색된 정보 활용 여부 확인
  - 답변 정확도 검증
  - 사용자 피드백 수집

#### 6.3 에러 처리
- [ ] **예외 상황 처리**
  - RDB 연결 실패
  - Vector Store 오류
  - 검색 결과 없음
  - 임베딩 생성 실패

---

### Phase 7: 최적화 및 개선 (지속적)

#### 7.1 검색 품질 개선
- [ ] **하이브리드 검색**
  - 키워드 검색 + 벡터 검색 결합
  - BM25 + 벡터 유사도 하이브리드
- [ ] **리랭킹 (Re-ranking)**
  - 검색 결과 재정렬
  - Cross-encoder 모델 활용

#### 7.2 데이터 관리
- [ ] **증분 업데이트**
  - 새로운 상담 데이터 자동 임베딩
  - 주기적 재임베딩 (데이터 변경 시)
- [ ] **데이터 정제**
  - 중복 제거
  - 품질 관리

#### 7.3 모니터링
- [ ] **검색 메트릭 수집**
  - 검색 성공률
  - 평균 검색 결과 수
  - 검색 결과 활용률
- [ ] **로그 분석**
  - 자주 검색되는 키워드
  - 검색 실패 케이스 분석

---

## 🛠️ 기술 스택

### 필수 의존성
- **Spring Data JPA**: RDB 연결 및 데이터 접근
- **Spring AI Vector Store**: 임베딩 저장 및 검색
  - PostgreSQL pgvector (프로덕션)
  - SimpleVectorStore (개발/테스트)
- **Spring AI Embedding**: 텍스트 임베딩
  - Google Gemini Embedding
- **데이터베이스 드라이버**: PostgreSQL, MySQL 등

### 선택적 의존성
- **Spring Batch**: 대량 데이터 임베딩
- **Redis**: 캐싱 (검색 결과 캐시)
- **Elasticsearch**: 하이브리드 검색 (선택사항)

---

## 📁 프로젝트 구조

```
src/main/java/ai/langgraph4j/msk/
├── agent/
│   ├── graph/
│   │   └── AgentGraph.java          # RAG 검색 노드 통합
│   ├── nodes/
│   │   ├── RagSearchNode.java        # ✨ 새로 추가: RAG 검색 노드
│   │   ├── InputNode.java
│   │   ├── LlmNode.java              # RAG 컨텍스트 포함하도록 수정
│   │   └── ...
│   └── state/
│       └── AgentState.java           # ragContext 필드 추가
├── config/
│   ├── AiConfig.java
│   ├── VectorStoreConfig.java        # ✨ 새로 추가: Vector Store 설정
│   └── ...
├── repository/
│   └── ConsultationRepository.java  # ✨ 새로 추가: 상담 데이터 Repository
├── entity/
│   └── Consultation.java            # ✨ 새로 추가: 상담 엔티티
├── service/
│   ├── ConsultationEmbeddingService.java  # ✨ 새로 추가: 임베딩 서비스
│   └── ConsultationRagService.java        # ✨ 새로 추가: RAG 검색 서비스
└── tools/
    └── ConsultationRagTool.java      # ✨ 새로 추가: RAG 검색 Tool
```

---

## 🔄 데이터 흐름

```
1. 사용자 질문 입력
   ↓
2. RagSearchNode
   - 사용자 질문을 임베딩으로 변환
   - Vector Store에서 유사도 검색
   - 상위 N개 상담 사례 조회
   ↓
3. AgentState에 검색 결과 저장 (ragContext)
   ↓
4. LlmNode
   - 검색된 상담 사례를 컨텍스트로 포함
   - System Prompt에 RAG 정보 활용 지시
   - LLM 호출
   ↓
5. ConditionalNode
   - 추가 Tool 필요 여부 확인
   ↓
6. ResponseNode
   - 최종 답변 생성 (검색된 정보 참조)
```

---

## 📊 우선순위별 작업 순서

### 1단계: 기반 구축 (1-2주)
1. ✅ RDB 연결 설정
2. ✅ 데이터 모델 설계
3. ✅ Vector Store 설정
4. ✅ 기본 임베딩 파이프라인

### 2단계: RAG 검색 구현 (1주)
5. ✅ RAG 검색 Tool 구현
6. ✅ 검색 결과 포맷팅
7. ✅ Tool 등록

### 3단계: 그래프 통합 (1주)
8. ✅ RagSearchNode 구현
9. ✅ 그래프 흐름 수정
10. ✅ 프롬프트 엔지니어링

### 4단계: 테스트 및 최적화 (1주)
11. ✅ 통합 테스트
12. ✅ 성능 최적화
13. ✅ 모니터링 설정

---

## 🎯 성공 기준

### 기능적 요구사항
- [ ] RDB에서 상담 데이터 조회 가능
- [ ] Vector Store에 임베딩 저장 및 검색 가능
- [ ] 사용자 질문에 대해 관련 상담 사례 검색 가능
- [ ] 검색된 정보를 우선적으로 활용한 답변 생성
- [ ] 검색 결과가 없을 경우 일반 LLM 응답

### 비기능적 요구사항
- [ ] 검색 응답 시간: 2초 이내
- [ ] 전체 응답 시간: 5초 이내
- [ ] 검색 정확도: 상위 5개 결과 중 관련성 80% 이상
- [ ] 시스템 안정성: 에러 발생 시 Graceful degradation

---

## 📝 체크리스트

### Phase 1: RDB 연결
- [ ] 의존성 추가
- [ ] 데이터베이스 연결 설정
- [ ] 상담 엔티티 설계
- [ ] Repository 구현
- [ ] 연결 테스트

### Phase 2: Vector Store
- [ ] Vector Store Bean 생성
- [ ] Embedding Model 설정
- [ ] 임베딩 서비스 구현
- [ ] 청킹 전략 구현
- [ ] 초기 데이터 로딩

### Phase 3: RAG Tool
- [ ] ConsultationRagTool 구현
- [ ] 검색 로직 구현
- [ ] 결과 포맷팅
- [ ] Tool 등록

### Phase 4: 그래프 통합
- [ ] RagSearchNode 구현
- [ ] AgentState 수정
- [ ] AgentGraph 수정
- [ ] LlmNode 수정 (RAG 컨텍스트 포함)
- [ ] 프롬프트 엔지니어링

### Phase 5: 테스트
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] 성능 테스트
- [ ] 정확도 검증

---

## 🚀 빠른 시작 가이드

### 1. 의존성 추가
```gradle
dependencies {
    // RDB 연결
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.postgresql:postgresql'
    
    // Vector Store
    implementation 'org.springframework.ai:spring-ai-pgvector-store'
    // 또는
    implementation 'org.springframework.ai:spring-ai-simple-vector-store'
}
```

### 2. 데이터베이스 설정
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/consultation_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Vector Store 설정
```java
@Bean
public VectorStore vectorStore(EmbeddingModel embeddingModel, DataSource dataSource) {
    return new PgVectorStore.Builder()
        .withEmbeddingModel(embeddingModel)
        .withDataSource(dataSource)
        .build();
}
```

### 4. RAG Tool 구현
```java
@Tool(description = "내부 상담 데이터베이스에서 관련 상담 내용을 검색합니다.")
public String searchConsultation(String query) {
    // 검색 로직
}
```

---

## 📚 참고 자료

- [Spring AI Vector Store 문서](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- [PostgreSQL pgvector 확장](https://github.com/pgvector/pgvector)
- [RAG (Retrieval-Augmented Generation) 패턴](https://www.promptingguide.ai/techniques/rag)
- [Spring Data JPA 문서](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

---

**마지막 업데이트**: 2025-01-XX
