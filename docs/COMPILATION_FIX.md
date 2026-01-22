# 컴파일 오류 수정 내역

## 📋 개요

이 문서는 프로젝트의 컴파일 오류를 수정한 내용을 정리한 것입니다. 주요 수정 사항은 다음과 같습니다:

1. 잘못된 import 제거
2. Spring AI Embedding 설정 변경 (auto-configuration 사용)
3. Lombok @Builder 경고 수정
4. PgVectorStore API 업데이트
5. Entity 필드명 수정

---

## 🔧 수정 상세 내역

### 1. Counsel.java - 잘못된 import 제거 및 @Builder.Default 추가

#### 문제점
- 존재하지 않는 패키지 `com.taxnet.entity.real.entity.account.Account` import
- Lombok @Builder 경고: `lawArticleCodes` 필드의 초기화 표현식이 무시됨

#### 수정 내용
```java
// ❌ 제거된 import
import com.taxnet.entity.real.entity.account.Account;

// ✅ 추가된 어노테이션
@Builder.Default
@ManyToMany
private List<LawArticleCode> lawArticleCodes = new ArrayList<>();
```

#### 파일 위치
- `src/main/java/ai/langgraph4j/msk/entity/counsel/Counsel.java`

---

### 2. Counselor.java - @Builder.Default 추가

#### 문제점
- Lombok @Builder 경고: List 필드들의 초기화 표현식이 무시됨
  - `representCounselCode`
  - `reConsultingCounselCode`
  - `reEtcCounselCode`

#### 수정 내용
```java
// ✅ 각 List 필드에 @Builder.Default 추가
@Builder.Default
@ManyToMany(cascade = CascadeType.ALL)
private List<CounselFieldLarge> representCounselCode = new ArrayList<>();

@Builder.Default
@ManyToMany(cascade = CascadeType.ALL)
private List<CounselFieldLarge> reConsultingCounselCode = new ArrayList<>();

@Builder.Default
@ManyToMany(cascade = CascadeType.ALL)
private List<CounselFieldLarge> reEtcCounselCode = new ArrayList<>();
```

#### 파일 위치
- `src/main/java/ai/langgraph4j/msk/entity/counsel/Counselor.java`

---

### 3. EmbeddingConfig.java - Spring AI Auto-Configuration 사용

#### 문제점
- 존재하지 않는 클래스 `GoogleGenAiEmbeddingModel` 사용 시도
- Spring AI 1.1.1에서 올바른 패키지 구조로 변경됨

#### 수정 내용

**이전 (수동 Bean 생성):**
```java
@Bean
public EmbeddingModel embeddingModel(Client genAiClient) {
    return new GoogleGenAiEmbeddingModel(genAiClient); // ❌ 클래스가 존재하지 않음
}
```

**수정 후 (Auto-Configuration 사용):**
```java
@Configuration
public class EmbeddingConfig {
    // Spring AI auto-configuration을 사용하므로 별도의 Bean 정의가 필요하지 않습니다.
    // application.properties에서 설정하면 자동으로 EmbeddingModel Bean이 생성됩니다.
}
```

#### 변경 이유
- Spring AI 1.1.1에서는 `spring-ai-starter-model-google-genai-embedding` 의존성을 추가하면 자동으로 `GoogleGenAiTextEmbeddingModel` Bean이 생성됩니다.
- 수동 설정보다 간단하고 유지보수가 용이합니다.

#### 파일 위치
- `src/main/java/ai/langgraph4j/msk/config/EmbeddingConfig.java`

---

### 4. build.gradle - Embedding 의존성 추가

#### 문제점
- Spring AI Embedding 관련 클래스를 찾을 수 없음
- `spring-ai-google-genai`는 Chat 모델용이고, Embedding용 별도 의존성이 필요함

#### 수정 내용
```gradle
// ✅ 추가된 의존성
// Spring AI Embedding (Gemini Embedding 사용)
implementation 'org.springframework.ai:spring-ai-starter-model-google-genai-embedding'
```

#### 파일 위치
- `build.gradle` (63번째 줄)

---

### 5. application.properties - Embedding 설정 추가

#### 추가된 설정
```properties
# Spring AI Embedding 설정
# Google GenAI Embedding을 사용합니다 (auto-configuration)
spring.ai.model.embedding.text=google-genai
spring.ai.google.genai.embedding.api-key=${GEMINI_API_KEY}
spring.ai.google.genai.embedding.text.options.model=text-embedding-004
spring.ai.google.genai.embedding.text.options.task-type=RETRIEVAL_DOCUMENT
```

#### 설정 설명
- `spring.ai.model.embedding.text=google-genai`: Google GenAI Embedding 모델 사용
- `spring.ai.google.genai.embedding.api-key`: API 키 설정 (환경 변수 사용)
- `spring.ai.google.genai.embedding.text.options.model`: Embedding 모델명 (text-embedding-004)
- `spring.ai.google.genai.embedding.text.options.task-type`: 작업 유형 (RETRIEVAL_DOCUMENT)

#### 파일 위치
- `src/main/resources/application.properties`

---

### 6. VectorStoreConfig.java - PgVectorStore API 업데이트

#### 문제점
- Spring AI 1.1.1에서 `PgVectorStore.builder()` API가 변경됨
- 이전 방식: `builder().withDataSource().withEmbeddingModel()...`
- 새로운 방식: `builder(JdbcTemplate, EmbeddingModel).dimensions()...`

#### 수정 내용

**이전:**
```java
@Bean
public VectorStore vectorStore(EmbeddingModel embeddingModel, DataSource dataSource) {
    PgVectorStore.Builder builder = PgVectorStore.builder()
        .withEmbeddingModel(embeddingModel)
        .withDataSource(dataSource)
        .withIndexType(PgVectorStore.PgIndexType.HNSW)
        .withDistanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
        .withDimensions(768)
        .withRemoveExistingVectorStoreTable(false);
    return builder.build();
}
```

**수정 후:**
```java
@Bean
public VectorStore vectorStore(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
    VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
        .dimensions(768) // Gemini Embedding 차원
        .initializeSchema(false) // 기존 테이블 유지
        .build();
    return vectorStore;
}
```

#### 주요 변경 사항
1. `DataSource` → `JdbcTemplate` 파라미터 변경
2. `builder()` → `builder(JdbcTemplate, EmbeddingModel)` 생성자 방식으로 변경
3. 메서드명 변경:
   - `withIndexType()` → 제거 (기본값 사용)
   - `withDistanceType()` → 제거 (기본값 사용)
   - `withRemoveExistingVectorStoreTable()` → `initializeSchema()`
   - `withDimensions()` → `dimensions()`

#### 파일 위치
- `src/main/java/ai/langgraph4j/msk/config/VectorStoreConfig.java`

---

### 7. ConsultationEmbeddingService.java - Entity 필드명 수정

#### 문제점
- `Counsel` Entity의 실제 필드명과 다른 메서드 호출
- 존재하지 않는 메서드 호출로 인한 컴파일 오류

#### 수정 내용

**이전 (잘못된 메서드명):**
```java
consultation.getTitle()        // ❌
consultation.getContent()      // ❌
consultation.getAnswer()       // ❌
consultation.getCategory()     // ❌
consultation.getCreatedAt()    // ❌
```

**수정 후 (올바른 메서드명):**
```java
consultation.getCounselTitle()      // ✅
consultation.getCounselContent()    // ✅
consultation.getAnswerContent()     // ✅
consultation.getCounselFieldLarge() // ✅ (category 대신)
consultation.getCounselAt()         // ✅
```

#### 필드 매핑
| 잘못된 메서드 | 올바른 메서드 | 필드명 |
|------------|------------|--------|
| `getTitle()` | `getCounselTitle()` | `counselTitle` |
| `getContent()` | `getCounselContent()` | `counselContent` |
| `getAnswer()` | `getAnswerContent()` | `answerContent` |
| `getCategory()` | `getCounselFieldLarge()` | `counselFieldLarge` |
| `getCreatedAt()` | `getCounselAt()` | `counselAt` |

#### 파일 위치
- `src/main/java/ai/langgraph4j/msk/service/ConsultationEmbeddingService.java`

---

## ✅ 수정 결과

### 컴파일 성공
모든 컴파일 오류가 해결되었으며, 프로젝트가 정상적으로 빌드됩니다.

```bash
./gradlew clean compileJava
# BUILD SUCCESSFUL
```

### 해결된 오류 목록
1. ✅ `package com.taxnet.entity.real.entity.account does not exist` - import 제거
2. ✅ `cannot find symbol: GoogleGenAiEmbeddingModel` - auto-configuration 사용
3. ✅ `@Builder will ignore the initializing expression` - @Builder.Default 추가
4. ✅ `method builder in class PgVectorStore cannot be applied` - API 업데이트
5. ✅ `cannot find symbol: method getTitle()` - 필드명 수정

---

## 📝 참고 사항

### Spring AI 1.1.1 변경 사항
- Embedding 모델은 별도의 starter 의존성이 필요합니다: `spring-ai-starter-model-google-genai-embedding`
- Auto-configuration을 사용하면 수동 Bean 설정이 불필요합니다
- PgVectorStore는 `JdbcTemplate`과 `EmbeddingModel`을 생성자에서 받습니다

### Lombok @Builder 사용 시 주의사항
- 초기화 표현식이 있는 필드는 `@Builder.Default` 어노테이션을 추가해야 합니다
- 그렇지 않으면 Builder가 초기화 표현식을 무시합니다

### Entity 필드명 확인
- Lombok의 `@Data` 어노테이션은 필드명을 기반으로 getter/setter를 생성합니다
- 필드명이 `counselTitle`이면 getter는 `getCounselTitle()`입니다

---

## 🔗 관련 문서
- [Spring AI Embedding Documentation](https://docs.spring.io/spring-ai/reference/api/embeddings/google-genai-embeddings-text.html)
- [Lombok @Builder Documentation](https://projectlombok.org/features/Builder)
- [Spring AI PgVectorStore Documentation](https://docs.spring.io/spring-ai/reference/api/vector-stores/pgvector-store.html)
