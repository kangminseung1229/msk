# 임베딩과 벡터 저장소 가이드

## 📚 개요

이 문서는 RAG 시스템에서 사용하는 **임베딩(Embedding)**과 **벡터 저장소(Vector Store)**에 대해 설명합니다.

---

## 🔤 임베딩(Embedding)이란?

### 개념
**임베딩(Embedding)**은 텍스트를 **고차원 벡터(숫자 배열)**로 변환하는 과정입니다.

- **입력**: 텍스트 (예: "상담 내용: 환불 요청")
- **출력**: 벡터 (예: `[0.123, -0.456, 0.789, ..., 0.234]` - 768차원)

### 왜 필요한가?
- **의미 기반 검색**: 키워드 매칭이 아닌 **의미 유사도**로 검색 가능
- **벡터 유사도 계산**: 두 텍스트의 의미가 얼마나 비슷한지 수치로 측정 가능
- **LLM과의 호환성**: LLM이 이해할 수 있는 형태로 변환

### 예시
```
질문: "환불하고 싶어요"
상담 1: "제품 환불 요청 처리 방법"
상담 2: "배송 지연 문의"

→ 임베딩 변환 후 유사도 계산:
질문과 상담 1의 유사도: 0.95 (매우 유사)
질문과 상담 2의 유사도: 0.23 (유사하지 않음)
```

---

## 🗄️ 벡터 저장소(Vector Store)란?

### 개념
**벡터 저장소**는 임베딩된 벡터를 저장하고, **유사도 검색**을 수행하는 데이터베이스입니다.

### 주요 기능
1. **벡터 저장**: 텍스트 임베딩을 저장
2. **유사도 검색**: 쿼리 벡터와 유사한 벡터를 찾아 반환
3. **메타데이터 저장**: 원본 텍스트, ID, 카테고리 등 저장

---

## 📍 벡터 저장소의 물리적 위치

### 1. PostgreSQL pgvector (권장 - 프로덕션)

#### 위치
- **PostgreSQL 데이터베이스 내부**에 저장됩니다
- 별도의 테이블에 벡터 데이터가 저장됨

#### 구조
```
PostgreSQL 데이터베이스
├── 일반 테이블 (consultations)
│   ├── id
│   ├── title
│   ├── content
│   └── answer
│
└── 벡터 테이블 (spring_ai_vector_store)
    ├── id
    ├── embedding (vector 타입) ← 여기에 벡터 저장
    ├── content (원본 텍스트)
    └── metadata (JSON)
```

#### 장점
- ✅ **영구 저장**: 데이터베이스에 저장되어 애플리케이션 재시작 후에도 유지
- ✅ **트랜잭션 지원**: 데이터 일관성 보장
- ✅ **백업/복구**: PostgreSQL 백업으로 벡터도 함께 백업
- ✅ **확장성**: 대용량 데이터 처리 가능
- ✅ **하이브리드 검색**: SQL 쿼리와 벡터 검색 결합 가능

#### 설정 방법
```sql
-- PostgreSQL에서 pgvector 확장 설치
CREATE EXTENSION IF NOT EXISTS vector;

-- Spring AI가 자동으로 테이블 생성
-- 테이블명: spring_ai_vector_store
```

#### 물리적 저장 위치
- PostgreSQL 데이터 디렉토리 (예: `/var/lib/postgresql/data/`)
- 운영체제 파일 시스템에 실제 데이터 파일로 저장
- 벡터는 PostgreSQL의 `vector` 타입으로 저장됨

---

### 2. SimpleVectorStore (개발/테스트용)

#### 위치
- **애플리케이션 메모리(RAM)**에 저장됩니다
- 애플리케이션 재시작 시 **모든 데이터가 사라집니다**

#### 구조
```java
// 메모리 내 HashMap으로 저장
Map<String, Document> documents = new HashMap<>();
```

#### 장점
- ✅ **설정 간단**: 별도 데이터베이스 불필요
- ✅ **빠른 개발**: 프로토타이핑에 적합
- ✅ **의존성 없음**: PostgreSQL 설치 불필요

#### 단점
- ❌ **데이터 손실**: 애플리케이션 재시작 시 데이터 사라짐
- ❌ **메모리 제한**: 대용량 데이터 처리 어려움
- ❌ **프로덕션 부적합**: 실제 서비스에는 사용 불가

---

### 3. 기타 벡터 저장소 옵션

#### Chroma
- 별도 서버로 실행되는 벡터 데이터베이스
- 위치: 별도 프로세스/서버

#### Pinecone
- 클라우드 서비스
- 위치: Pinecone 클라우드 서버

#### Weaviate
- 별도 서버로 실행
- 위치: 별도 프로세스/서버

---

## 🔄 임베딩 프로세스

### 1. 텍스트 → 임베딩 변환

```java
// Spring AI EmbeddingModel 사용
EmbeddingModel embeddingModel = new GoogleGenAiEmbeddingModel(genAiClient);

// 텍스트를 벡터로 변환
String text = "상담 내용: 환불 요청 처리 방법";
List<Double> embedding = embeddingModel.embed(text);

// 결과: [0.123, -0.456, 0.789, ..., 0.234] (768차원)
```

### 2. 임베딩 → 벡터 저장소 저장

```java
// Vector Store에 저장
VectorStore vectorStore = new PgVectorStore(...);

Document document = new Document(
    text,                    // 원본 텍스트
    metadata,                // 메타데이터 (ID, 카테고리 등)
    embedding                // 임베딩 벡터
);

vectorStore.add(List.of(document));
```

### 3. 검색: 쿼리 → 유사 벡터 찾기

```java
// 사용자 질문을 임베딩으로 변환
String query = "환불하고 싶어요";
List<Double> queryEmbedding = embeddingModel.embed(query);

// 유사한 벡터 검색
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query(queryEmbedding)
        .topK(5)              // 상위 5개 결과
        .similarityThreshold(0.7)  // 유사도 0.7 이상
        .build()
);
```

---

## 🛠️ 구현 예시

### 1. Embedding Model 설정

```java
@Configuration
public class EmbeddingConfig {
    
    @Bean
    public EmbeddingModel embeddingModel(Client genAiClient) {
        // Gemini Embedding 사용
        return new GoogleGenAiEmbeddingModel(genAiClient);
    }
}
```

### 2. Vector Store 설정

```java
@Configuration
public class VectorStoreConfig {
    
    @Bean
    public VectorStore vectorStore(
            EmbeddingModel embeddingModel,
            DataSource dataSource) {
        
        return new PgVectorStore.Builder()
            .withEmbeddingModel(embeddingModel)
            .withDataSource(dataSource)
            .withIndexType(PgVectorStore.PgIndexType.HNSW)
            .withDistanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
            .withDimensions(768)  // Gemini Embedding 차원
            .withRemoveExistingVectorStoreTable(true)  // 개발용: 기존 테이블 삭제
            .build();
    }
}
```

### 3. 데이터 임베딩 및 저장

```java
@Service
public class ConsultationEmbeddingService {
    
    private final ConsultationRepository consultationRepository;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    
    public void embedAndStore(Long consultationId) {
        // 1. RDB에서 상담 데이터 조회
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow();
        
        // 2. 텍스트 준비 (제목 + 내용)
        String text = consultation.getTitle() + "\n" + consultation.getContent();
        
        // 3. 임베딩 생성
        List<Double> embedding = embeddingModel.embed(text);
        
        // 4. 메타데이터 준비
        Map<String, Object> metadata = Map.of(
            "consultationId", consultation.getId(),
            "category", consultation.getCategory(),
            "createdAt", consultation.getCreatedAt().toString()
        );
        
        // 5. Document 생성
        Document document = new Document(
            text,
            metadata,
            embedding
        );
        
        // 6. Vector Store에 저장
        vectorStore.add(List.of(document));
    }
}
```

---

## 📊 데이터 흐름도

```
1. 상담 데이터 (RDB)
   ↓
2. 텍스트 추출
   "제목: 환불 요청\n내용: 환불하고 싶습니다"
   ↓
3. Embedding Model
   [0.123, -0.456, 0.789, ..., 0.234] (768차원 벡터)
   ↓
4. Vector Store (PostgreSQL)
   ┌─────────────────────────────────┐
   │ spring_ai_vector_store 테이블   │
   │ - id                            │
   │ - embedding (vector)            │
   │ - content (text)                │
   │ - metadata (jsonb)              │
   └─────────────────────────────────┘
   ↓
5. 검색 시
   사용자 질문 → 임베딩 → 유사도 검색 → 관련 상담 반환
```

---

## 🔍 검색 과정 상세

### 1. 사용자 질문 입력
```
"환불하고 싶어요"
```

### 2. 질문 임베딩
```java
List<Double> queryEmbedding = embeddingModel.embed("환불하고 싶어요");
// 결과: [0.125, -0.451, 0.785, ..., 0.231]
```

### 3. Vector Store에서 유사도 검색
```sql
-- PostgreSQL pgvector가 내부적으로 수행하는 작업
SELECT 
    content,
    metadata,
    embedding <=> '[0.125, -0.451, ...]'::vector AS distance
FROM spring_ai_vector_store
ORDER BY distance ASC
LIMIT 5;
```

### 4. 결과 반환
```java
[
    {
        content: "제목: 환불 요청\n내용: 환불 처리 방법",
        metadata: {consultationId: 123, category: "환불"},
        similarity: 0.95
    },
    {
        content: "제목: 환불 정책\n내용: 환불 가능 기간",
        metadata: {consultationId: 456, category: "정책"},
        similarity: 0.87
    },
    ...
]
```

---

## 💾 저장 공간

### PostgreSQL pgvector

#### 벡터 크기 계산
- **차원**: 768 (Gemini Embedding)
- **데이터 타입**: `float4` (4 bytes)
- **벡터당 크기**: 768 × 4 bytes = **3,072 bytes ≈ 3 KB**

#### 예시
- 상담 데이터 10,000건
- 각 상담당 평균 3개 청크 (chunk)
- 총 벡터 수: 30,000개
- **총 저장 공간**: 30,000 × 3 KB = **90 MB**

#### 인덱스 공간
- HNSW 인덱스: 벡터 데이터의 약 20-30% 추가 공간 필요
- **총 예상 공간**: 90 MB + 27 MB = **약 120 MB**

---

## ⚙️ 설정 요약

### application.properties
```properties
# PostgreSQL 연결
spring.datasource.url=jdbc:postgresql://localhost:5432/consultation_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# pgvector 설정
spring.ai.vectorstore.pgvector.index-type=HNSW
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.dimensions=768
```

### PostgreSQL 확장 설치
```sql
-- PostgreSQL에서 실행
CREATE EXTENSION IF NOT EXISTS vector;
```

---

## 🎯 결론

### 임베딩
- **역할**: 텍스트를 벡터로 변환
- **도구**: Spring AI EmbeddingModel (Gemini Embedding)
- **위치**: 변환 과정 (메모리에서 수행)

### 벡터 저장소
- **PostgreSQL pgvector (권장)**
  - 위치: PostgreSQL 데이터베이스 내부
  - 물리적 저장: PostgreSQL 데이터 디렉토리
  - 영구 저장: ✅
  
- **SimpleVectorStore (개발용)**
  - 위치: 애플리케이션 메모리
  - 영구 저장: ❌

---

**마지막 업데이트**: 2025-01-XX
