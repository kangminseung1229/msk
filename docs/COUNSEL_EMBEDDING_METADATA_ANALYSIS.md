# 상담 임베딩 메타데이터 원인 분석

## 현상

DB에 저장된 상담 임베딩 메타데이터(`datasample/vector/metadata.json` 샘플)가 다음과 같이 **기본 5개 필드만** 존재함:

```json
{
    "title": "지방소득세(특별징수분) 가산세 문의",
    "counselId": 49144,
    "createdAt": "2011-11-07T16:42:19.553",
    "fieldLarge": "지방세",
    "documentType": "counsel"
}
```

설계상 `CounselMetadata` 형태로 저장하려 했으나, `lawArticleKeys`, `lawArticles`, `lawIds`, `articleKeys`, `lawArticlePairs`, `chunkIndex`, `totalChunks` 등이 없음.

---

## 설계 vs 구현

### 1. 저장 로직 (현재 코드)

- **위치**: `CounselEmbeddingService.embedConsultationsBatch()`
- **흐름**: `Counsel` → `CounselMetadata.from(consultation)` → `metadata.toMap()` → `Document(chunk, map)` → `vectorStore.add(documents)`
- **결론**: 저장 시 **CounselMetadata.toMap()** 을 사용하고 있어, 설계대로 동작함.

### 2. CounselMetadata.toMap() 동작

`CounselMetadata.toMap()` 은 **null/empty 인 필드는 map 에 넣지 않음**:

| 필드 | 조건으로 map 포함 |
|------|-------------------|
| `documentType` | 항상 |
| `counselId`, `title`, `fieldLarge`, `createdAt` | non-null 일 때 |
| `lawArticleKeys`, `lawArticles`, `lawIds`, `articleKeys`, `lawArticlePairs` | non-null **그리고** non-empty 일 때 |
| `chunkIndex`, `totalChunks` | non-null 일 때 |

따라서 **연관 법령이 없고, 단일 청크**인 상담은 위 JSON 과 동일하게 **기본 5개 필드만** 저장되는 것이 **현재 구현과 일치**함.

### 3. 청크 정보 저장 조건

`CounselEmbeddingService` 에서:

```java
if (chunks.size() > 1) {
    metadata.setChunkIndex(i);
    metadata.setTotalChunks(chunks.size());
}
```

- **단일 청크** (`chunks.size() == 1`) 인 경우 `chunkIndex`, `totalChunks` 를 설정하지 않음 → `toMap()` 에서 제외됨.

---

## 원인 정리

### 1) 재임베딩 미실시 (가장 유력)

- `CounselMetadata` 에 `lawArticleKeys`, `lawArticles`, `lawIds`, `articleKeys`, `lawArticlePairs` 등이 추가된 **이후**,  
  **전체 재임베딩(`reembedAllConsultations`)** 을 돌리지 않았을 가능성이 큼.
- 현재 DB 상담 임베딩은 **과거에 구 로직으로 생성된 벡터**가 그대로 남아 있는 상태일 수 있음.
- 구 로직은 `Map.of("counselId", ...)` 등 **최소 메타데이터**만 넣었을 가능성 있음 (예: `docs/EMBEDDING_AND_VECTOR_STORE.md` 예시).

### 2) 해당 상담(49144)의 데이터 특성

- **연관 법령 없음**: `lawArticleCodes` 가 null 또는 empty 이면, `CounselMetadata.from()` 에서 law* 필드를 채우지 않고, `toMap()` 에서도 제외됨.
- **단일 청크**: 내용이 짧아 1개 청크면 `chunkIndex`/`totalChunks` 미설정 → 역시 제외.
- 따라서 **지금 코드로 49144를 다시 임베딩해도** 동일하게 `title`, `counselId`, `createdAt`, `fieldLarge`, `documentType` 5개만 나오는 것이 **정상**임.

### 3) 과거 임베딩 예시/스크립트

- `docs/EMBEDDING_AND_VECTOR_STORE.md` 에는  
  `Map.of("counselId", ..., "category", ..., "createdAt", ...)` 형태의 **단순 메타데이터** 예시가 있음.
- 과거에 비슷한 **별도 스크립트/배치**로 임베딩했다면, 그 결과가 DB 에 남아 있을 수 있음.  
  (실제 DB 에는 `fieldLarge` 가 있어, 위 예시와 완전 동일한 구 코드인지는 추가 확인 필요.)

---

## 결론

| 구분 | 내용 |
|------|------|
| **저장 로직** | `CounselEmbeddingService` 는 `CounselMetadata.from()` + `toMap()` 사용 → **CounselMetadata 형태로 저장하도록 설계된 대로 동작**함. |
| **DB 에 5개 필드만 있는 이유** | (1) **재임베딩 미실시**로 구 메타데이터 구조가 남아 있거나, (2) 해당 상담이 **연관 법령 없음 + 단일 청크**라서 **현재 로직으로도** 5개 필드만 저장되는 경우. |
| **“예전 로직” 느낌의 원인** | 실제 DB 벡터가 **과거 임베딩 결과**이기 때문일 가능성이 높음. |

---

## 권장 조치

1. **counsel 49144 검증**
   - DB 에서 `counsel` ↔ `law_article_code` 연관 존재 여부 확인.
   - 연관이 **없다면** → 현재 코드로 재임베딩해도 5개 필드만 나오는 것이 맞음.
   - 연관이 **있다면** → `lawArticleCodes` 로딩(fetch) 이나 `CounselMetadata.from()` 사용처 추가 점검.

2. **전체 재임베딩**
   - `deleteEmbeddingsBycounselId` / `deleteAllEmbeddings` 구현 후  
     `reembedAllConsultations` 실행 (`docs/DELETE_COUNSEL_EMBEDDINGS.sql` 참고).
   - 연관 법령이 있는 상담은 `lawArticleKeys`, `lawArticles`, `lawIds`, `articleKeys`, `lawArticlePairs` 가 포함된 **CounselMetadata** 형태로 다시 적재됨.

3. **(선택) 메타데이터 스키마 통일**
   - 연관 법령이 없어도 `lawArticleKeys: []`, `lawArticles: []` 등 **빈 리스트**를 명시적으로 저장해,  
     검색/필터링 로직에서 “키 없음”과 “빈 배열”을 구분하고 싶다면 `toMap()` 수정 검토.

---

## 참고

- `CounselMetadata`: `src/main/java/ai/langgraph4j/aiagent/metadata/CounselMetadata.java`
- `CounselEmbeddingService`: `src/main/java/ai/langgraph4j/aiagent/service/CounselEmbeddingService.java`
- 메타데이터 구조: `docs/METADATA_STRUCTURE.md`
- 상담 임베딩 삭제 SQL: `docs/DELETE_COUNSEL_EMBEDDINGS.sql`
