# 법령 조문 벡터 임베딩 로드맵

**목표**: 최신 법령 조문들을 벡터 스토어에 추가하여 법적 근거를 제공할 수 있도록 개선

**작성일**: 2025-01-23

---

## 📋 개요

### 현재 상황
- ✅ 상담 데이터(Counsel)만 벡터 스토어에 임베딩되어 있음
- ✅ `LawArticleCode` 엔티티는 존재하지만 벡터 스토어에 포함되지 않음
- ❌ 법적 근거를 제공하기 어려운 상황

### 목표
- 법령 조문 데이터를 벡터 스토어에 임베딩
- 상담 검색 시 관련 법령 조문도 함께 검색 가능하도록 개선
- 검색 결과에서 법적 근거를 명확히 제시

### 핵심 요구사항
1. **법령 조문 데이터 소스 확보**: 법령 조문 내용을 가져올 수 있는 방법 확립
2. **법령 조문 임베딩 서비스**: 법령 조문을 벡터로 변환하여 저장
3. **통합 검색 서비스**: 상담 데이터와 법령 조문을 함께 검색
4. **메타데이터 구분**: 검색 결과에서 상담 데이터와 법령 조문을 구분
5. **법령 업데이트 파이프라인**: 최신 법령 조문을 주기적으로 업데이트

---

## 🗺️ 단계별 로드맵

### Phase 1: 법령 조문 데이터 모델 및 소스 확보 (1주)

#### 1.1 법령 조문 데이터 구조 분석
- [ ] **현재 `LawArticleCode` 엔티티 분석**
  - `lawId`: 법령 ID
  - `articleKey`: 조문 키 (예: "0001-01")
  - 실제 조문 내용은 별도 저장소에 있을 가능성

- [ ] **법령 조문 내용 저장 위치 확인**
  - 옵션 1: 별도 테이블에 조문 내용 저장
  - 옵션 2: 외부 API에서 실시간 조회 (법제처 API 등)
  - 옵션 3: 파일 시스템에 저장된 법령 데이터

#### 1.2 법령 조문 엔티티 설계 (필요시)
- [ ] **법령 조문 내용을 저장할 엔티티 생성** (조문 내용이 DB에 없는 경우)
  ```java
  @Entity
  public class LawArticle {
      @Id
      private Long id;
      
      @ManyToOne
      private LawArticleCode lawArticleCode; // LawArticleCode와 연결
      
      @Column(columnDefinition = "TEXT")
      private String articleContent; // 조문 내용
      
      private String lawName; // 법령명
      private String articleNumber; // 조문 번호 (예: "제1조")
      private LocalDateTime effectiveDate; // 시행일
      private LocalDateTime lastUpdated; // 최종 수정일
      
      // ... 기타 필드
  }
  ```

- [ ] **Repository 인터페이스 생성**
  ```java
  public interface LawArticleRepository extends JpaRepository<LawArticle, Long> {
      List<LawArticle> findByLawArticleCode(LawArticleCode code);
      List<LawArticle> findByLawId(String lawId);
      // 최신 법령만 조회하는 메서드
      List<LawArticle> findTopByOrderByLastUpdatedDesc(LocalDateTime since);
  }
  ```

#### 1.3 법령 조문 데이터 소스 연동
- [ ] **외부 API 연동** (법제처 API 등)
  - 한국 법령정보센터 API 연동
  - 법령 조문 조회 서비스 구현
  - API 키 및 인증 설정

- [ ] **또는 내부 데이터베이스 연동**
  - 기존 법령 데이터베이스 확인
  - 데이터 마이그레이션 계획 수립

---

### Phase 2: 법령 조문 임베딩 서비스 구현 (1주)

#### 2.1 법령 조문 임베딩 서비스 생성
- [ ] **`LawArticleEmbeddingService` 구현**
  ```java
  @Service
  @RequiredArgsConstructor
  public class LawArticleEmbeddingService {
      
      private final LawArticleRepository lawArticleRepository;
      private final VectorStore vectorStore;
      
      /**
       * 모든 법령 조문을 임베딩하여 Vector Store에 저장
       */
      @Transactional
      public int embedAllLawArticles() {
          // 배치 처리로 법령 조문 임베딩
      }
      
      /**
       * 특정 법령 조문을 임베딩
       */
      @Transactional
      public void embedLawArticle(Long lawArticleId) {
          // 단일 법령 조문 임베딩
      }
      
      /**
       * 최신 법령 조문만 임베딩 (증분 업데이트)
       */
      @Transactional
      public int embedRecentLawArticles(LocalDateTime since) {
          // 특정 시점 이후의 법령만 임베딩
      }
  }
  ```

#### 2.2 법령 조문 텍스트 구성 전략
- [ ] **임베딩할 텍스트 구성**
  ```java
  private String buildLawArticleText(LawArticle lawArticle) {
      StringBuilder text = new StringBuilder();
      
      // 법령명
      text.append("법령명: ").append(lawArticle.getLawName()).append("\n");
      
      // 조문 번호
      text.append("조문: ").append(lawArticle.getArticleNumber()).append("\n");
      
      // 조문 내용
      text.append("내용: ").append(lawArticle.getArticleContent()).append("\n");
      
      // 시행일 (최신성 정보)
      if (lawArticle.getEffectiveDate() != null) {
          text.append("시행일: ").append(lawArticle.getEffectiveDate()).append("\n");
      }
      
      return text.toString();
  }
  ```

#### 2.3 메타데이터 설계
- [ ] **법령 조문 메타데이터 구성**
  ```java
  private Map<String, Object> buildLawArticleMetadata(LawArticle lawArticle) {
      Map<String, Object> metadata = new HashMap<>();
      
      metadata.put("documentType", "LAW_ARTICLE"); // 상담과 구분
      metadata.put("lawArticleId", lawArticle.getId());
      metadata.put("lawId", lawArticle.getLawArticleCode().getLawId());
      metadata.put("articleKey", lawArticle.getLawArticleCode().getArticleKey());
      metadata.put("lawName", lawArticle.getLawName());
      metadata.put("articleNumber", lawArticle.getArticleNumber());
      metadata.put("effectiveDate", lawArticle.getEffectiveDate().toString());
      metadata.put("lastUpdated", lawArticle.getLastUpdated().toString());
      
      return metadata;
  }
  ```

#### 2.4 청킹 전략
- [ ] **법령 조문 청킹 전략**
  - 조문 단위로 분할 (각 조문은 보통 짧으므로 청킹 불필요할 수 있음)
  - 긴 조문의 경우 문장 단위로 분할
  - 조문 번호와 법령명은 각 청크에 포함

---

### Phase 3: 통합 검색 서비스 구현 (1주)

#### 3.1 통합 검색 서비스 생성
- [ ] **`UnifiedSearchService` 구현**
  ```java
  @Service
  @RequiredArgsConstructor
  public class UnifiedSearchService {
      
      private final ConsultationSearchService consultationSearchService;
      private final LawArticleSearchService lawArticleSearchService;
      
      /**
       * 상담 데이터와 법령 조문을 함께 검색
       */
      public UnifiedSearchResult search(String query, int topK) {
          // 상담 데이터 검색
          List<SearchResult> consultations = 
              consultationSearchService.search(query, topK);
          
          // 법령 조문 검색
          List<LawArticleSearchResult> lawArticles = 
              lawArticleSearchService.search(query, topK);
          
          return UnifiedSearchResult.builder()
              .consultations(consultations)
              .lawArticles(lawArticles)
              .build();
      }
      
      /**
       * 가중치 기반 통합 검색
       * 상담 데이터와 법령 조문의 유사도 점수를 조정하여 통합
       */
      public List<UnifiedSearchResult> searchWithWeight(
          String query, 
          int topK,
          double consultationWeight,
          double lawArticleWeight
      ) {
          // 가중치를 적용한 통합 검색
      }
  }
  ```

#### 3.2 법령 조문 전용 검색 서비스
- [ ] **`LawArticleSearchService` 구현**
  ```java
  @Service
  @RequiredArgsConstructor
  public class LawArticleSearchService {
      
      private final VectorStore vectorStore;
      
      /**
       * 법령 조문만 검색 (documentType 필터링)
       */
      public List<LawArticleSearchResult> search(String query, int topK) {
          SearchRequest searchRequest = SearchRequest.builder()
              .query(query)
              .topK(topK)
              .similarityThreshold(0.6)
              .filterExpression("documentType == 'LAW_ARTICLE'") // 메타데이터 필터
              .build();
          
          List<Document> documents = vectorStore.similaritySearch(searchRequest);
          
          return documents.stream()
              .map(this::convertToLawArticleSearchResult)
              .toList();
      }
  }
  ```

#### 3.3 검색 결과 DTO 설계
- [ ] **통합 검색 결과 DTO**
  ```java
  @Data
  @Builder
  public class UnifiedSearchResult {
      private List<SearchResult> consultations; // 상담 데이터
      private List<LawArticleSearchResult> lawArticles; // 법령 조문
      
      // 검색 결과를 LLM에 전달할 형식으로 포맷팅
      public String formatForContext() {
          StringBuilder context = new StringBuilder();
          
          // 상담 데이터 섹션
          if (!consultations.isEmpty()) {
              context.append("=== 관련 상담 사례 ===\n");
              for (SearchResult consultation : consultations) {
                  context.append(formatConsultation(consultation)).append("\n\n");
              }
          }
          
          // 법령 조문 섹션
          if (!lawArticles.isEmpty()) {
              context.append("=== 관련 법령 조문 ===\n");
              for (LawArticleSearchResult lawArticle : lawArticles) {
                  context.append(formatLawArticle(lawArticle)).append("\n\n");
              }
          }
          
          return context.toString();
      }
  }
  
  @Data
  @Builder
  public class LawArticleSearchResult {
      private Long lawArticleId;
      private String lawName;
      private String articleNumber;
      private String articleContent;
      private String articleKey;
      private LocalDateTime effectiveDate;
      private Double similarityScore;
  }
  ```

---

### Phase 4: 검색 로직 개선 및 통합 (1주)

#### 4.1 검색 전략 개선
- [ ] **2단계 검색 전략**
  1. 1단계: 사용자 질문으로 상담 데이터와 법령 조문 동시 검색
  2. 2단계: 검색된 상담 데이터의 `lawArticleCodes`를 활용하여 관련 법령 추가 검색

- [ ] **하이브리드 검색**
  - 벡터 검색 + 키워드 검색 (법령명, 조문 번호 등)
  - BM25 + 벡터 유사도 결합

#### 4.2 SearchTool 수정
- [ ] **`SearchTool`에 법령 조문 검색 추가**
  ```java
  @Component
  public class SearchTool {
      
      private final UnifiedSearchService unifiedSearchService;
      
      @Tool(description = "상담 데이터와 법령 조문을 검색합니다. " +
            "법적 근거가 필요한 경우 관련 법령 조문도 함께 반환됩니다.")
      public String search(
          @ToolParam(description = "검색할 질문 또는 키워드") String query
      ) {
          UnifiedSearchResult result = 
              unifiedSearchService.search(query, 10);
          
          return result.formatForContext();
      }
  }
  ```

#### 4.3 GeminiTextService 수정
- [ ] **법령 조문을 포함한 컨텍스트 제공**
  ```java
  // GeminiTextService에서
  var searchResults = unifiedSearchService.search(userPrompt, 10);
  
  String searchContext = searchResults.formatForContext();
  
  // System Instruction에 법령 조문 활용 지시 추가
  String systemInstruction = buildSystemInstruction(
      baseSystemInstruction, 
      searchContext
  );
  ```

#### 4.4 System Prompt 개선
- [ ] **법령 조문 활용 지시 추가**
  ```
  당신은 법률 상담 전문 AI 어시스턴트입니다.
  
  답변 시 다음 원칙을 따르세요:
  1. 검색된 상담 사례를 우선적으로 참고하세요
  2. **법적 근거가 필요한 경우, 검색된 법령 조문을 반드시 인용하세요**
  3. 법령 조문을 인용할 때는 법령명과 조문 번호를 명시하세요
  4. 최신 법령 조문을 우선적으로 사용하세요
  
  [검색된 상담 사례]
  {consultations}
  
  [검색된 법령 조문]
  {lawArticles}
  ```

---

### Phase 5: 법령 업데이트 파이프라인 (1주)

#### 5.1 법령 업데이트 스케줄러
- [ ] **주기적 법령 업데이트 작업**
  ```java
  @Component
  @RequiredArgsConstructor
  public class LawArticleUpdateScheduler {
      
      private final LawArticleEmbeddingService lawArticleEmbeddingService;
      private final LawArticleRepository lawArticleRepository;
      
      /**
       * 매일 새벽에 최신 법령 조문 확인 및 임베딩
       */
      @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
      public void updateRecentLawArticles() {
          LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
          
          // 최근 업데이트된 법령 조문 조회
          List<LawArticle> recentArticles = 
              lawArticleRepository.findByLastUpdatedAfter(yesterday);
          
          if (!recentArticles.isEmpty()) {
              log.info("최신 법령 조문 {}건 발견, 임베딩 시작", recentArticles.size());
              
              // 기존 임베딩 삭제 후 재임베딩
              for (LawArticle article : recentArticles) {
                  lawArticleEmbeddingService.deleteAndReembed(article.getId());
              }
          }
      }
  }
  ```

#### 5.2 법령 데이터 동기화
- [ ] **외부 법령 데이터와 동기화**
  - 법제처 API에서 최신 법령 정보 조회
  - 변경된 법령 조문 감지
  - 자동 업데이트 또는 알림

#### 5.3 배치 작업 설정
- [ ] **초기 법령 데이터 로딩**
  - 전체 법령 조문 일괄 임베딩
  - 진행 상황 모니터링
  - 에러 처리 및 재시도 로직

---

### Phase 6: 검색 품질 개선 (1주)

#### 6.1 리랭킹 (Re-ranking)
- [ ] **검색 결과 재정렬**
  - Cross-encoder 모델을 활용한 정확도 향상
  - 법령 조문의 경우 법령명/조문 번호 매칭 가중치 추가

#### 6.2 검색 결과 필터링
- [ ] **유효한 법령만 검색**
  - 폐지된 법령 제외
  - 시행일 기준 필터링
  - 최신 법령 우선순위

#### 6.3 검색 결과 포맷팅 개선
- [ ] **법령 조문 표시 형식**
  ```
  [법령명] 제X조 (시행일: YYYY-MM-DD)
  조문 내용...
  
  유사도: 0.85
  ```

---

### Phase 7: 테스트 및 검증 (1주)

#### 7.1 단위 테스트
- [ ] 법령 조문 임베딩 서비스 테스트
- [ ] 법령 조문 검색 서비스 테스트
- [ ] 통합 검색 서비스 테스트

#### 7.2 통합 테스트
- [ ] 전체 검색 플로우 테스트
- [ ] 법령 조문이 포함된 답변 생성 테스트
- [ ] 성능 테스트 (검색 속도, 응답 시간)

#### 7.3 정확도 검증
- [ ] 법령 조문 검색 정확도 평가
- [ ] 법적 근거 제공 품질 검증
- [ ] 사용자 피드백 수집

---

## 🛠️ 기술 스택

### 추가 필요 의존성
- **법제처 API 클라이언트** (또는 내부 법령 데이터베이스)
- **스케줄러**: Spring `@Scheduled` 또는 Quartz
- **리랭킹 모델** (선택사항): Cross-encoder 모델

---

## 📁 프로젝트 구조

```
src/main/java/ai/langgraph4j/aiagent/
├── entity/
│   └── law/
│       ├── LawArticleCode.java          # 기존
│       └── LawArticle.java              # ✨ 새로 추가: 조문 내용 저장
│
├── repository/
│   ├── CounselRepository.java           # 기존
│   └── LawArticleRepository.java        # ✨ 새로 추가
│
├── service/
│   ├── ConsultationEmbeddingService.java    # 기존
│   ├── ConsultationSearchService.java       # 기존
│   ├── LawArticleEmbeddingService.java      # ✨ 새로 추가
│   ├── LawArticleSearchService.java         # ✨ 새로 추가
│   └── UnifiedSearchService.java            # ✨ 새로 추가: 통합 검색
│
├── service/dto/
│   ├── SearchResult.java                # 기존
│   ├── LawArticleSearchResult.java      # ✨ 새로 추가
│   └── UnifiedSearchResult.java         # ✨ 새로 추가
│
├── tools/
│   └── SearchTool.java                  # 수정: 법령 조문 검색 추가
│
└── scheduler/
    └── LawArticleUpdateScheduler.java   # ✨ 새로 추가: 법령 업데이트 스케줄러
```

---

## 🔄 데이터 흐름

### 법령 조문 임베딩 흐름
```
1. 법령 조문 데이터 소스
   (DB 또는 외부 API)
   ↓
2. LawArticleEmbeddingService
   - 법령 조문 텍스트 구성
   - 임베딩 생성
   - 메타데이터 추가 (documentType: "LAW_ARTICLE")
   ↓
3. Vector Store 저장
   - 상담 데이터와 동일한 벡터 스토어 사용
   - 메타데이터로 구분
```

### 통합 검색 흐름
```
1. 사용자 질문 입력
   ↓
2. UnifiedSearchService
   ├─ ConsultationSearchService
   │  └─ 상담 데이터 검색 (documentType: "CONSULTATION")
   │
   └─ LawArticleSearchService
      └─ 법령 조문 검색 (documentType: "LAW_ARTICLE")
   ↓
3. 검색 결과 통합 및 포맷팅
   ↓
4. LLM에 컨텍스트 제공
   - 상담 사례 + 법령 조문
   ↓
5. 법적 근거를 포함한 답변 생성
```

---

## 📊 우선순위별 작업 순서

### 1단계: 기반 구축 (1-2주)
1. 법령 조문 데이터 소스 확보
2. 법령 조문 엔티티 및 Repository 구현
3. 법령 조문 임베딩 서비스 구현
4. 초기 법령 데이터 로딩

### 2단계: 검색 기능 구현 (1주)
5. 법령 조문 검색 서비스 구현
6. 통합 검색 서비스 구현
7. SearchTool 수정

### 3단계: 통합 및 개선 (1주)
8. GeminiTextService 수정
9. System Prompt 개선
10. 검색 품질 개선

### 4단계: 자동화 및 최적화 (1주)
11. 법령 업데이트 스케줄러 구현
12. 성능 최적화
13. 테스트 및 검증

---

## 🎯 성공 기준

### 기능적 요구사항
- [ ] 법령 조문을 벡터 스토어에 임베딩 가능
- [ ] 상담 검색 시 관련 법령 조문도 함께 검색 가능
- [ ] 검색 결과에서 상담 데이터와 법령 조문을 구분 가능
- [ ] 법적 근거를 포함한 답변 생성 가능
- [ ] 최신 법령 조문을 주기적으로 업데이트 가능

### 비기능적 요구사항
- [ ] 법령 조문 검색 응답 시간: 2초 이내
- [ ] 통합 검색 응답 시간: 3초 이내
- [ ] 법령 조문 검색 정확도: 상위 5개 결과 중 관련성 80% 이상
- [ ] 법령 업데이트 작업: 백그라운드에서 수행, 사용자 영향 없음

---

## ⚠️ 고려사항

### 1. 법령 조문 데이터 소스
- 법령 조문 내용이 어디에 저장되어 있는지 확인 필요
- 외부 API 사용 시 API 호출 제한 및 비용 고려
- 법령 데이터의 최신성 보장 방법

### 2. 벡터 스토어 용량
- 법령 조문 수가 많을 경우 저장 공간 증가
- 예상: 법령 조문 10,000건 × 평균 3KB = 약 30MB
- 인덱스 성능 고려

### 3. 검색 성능
- 상담 데이터와 법령 조문을 함께 검색할 때 성능 영향
- 메타데이터 필터링 성능
- 필요시 별도 벡터 스토어 분리 고려

### 4. 법령 업데이트
- 법령이 변경되면 기존 임베딩 삭제 후 재임베딩 필요
- 변경 감지 방법 (외부 API 폴링, 이벤트 기반 등)
- 업데이트 중 검색 서비스 가용성

### 5. 법적 책임
- AI가 제공하는 법적 근거의 정확성 보장
- 법령 해석에 대한 책임 명시
- 최신 법령 정보 제공 의무

---

## 📝 체크리스트

### Phase 1: 데이터 모델 및 소스
- [ ] 법령 조문 데이터 소스 확인
- [ ] LawArticle 엔티티 설계 및 구현
- [ ] LawArticleRepository 구현
- [ ] 외부 API 연동 (필요시)

### Phase 2: 임베딩 서비스
- [ ] LawArticleEmbeddingService 구현
- [ ] 법령 조문 텍스트 구성 로직
- [ ] 메타데이터 설계 및 구현
- [ ] 초기 법령 데이터 로딩

### Phase 3: 검색 서비스
- [ ] LawArticleSearchService 구현
- [ ] UnifiedSearchService 구현
- [ ] 검색 결과 DTO 설계
- [ ] 통합 검색 테스트

### Phase 4: 통합
- [ ] SearchTool 수정
- [ ] GeminiTextService 수정
- [ ] System Prompt 개선
- [ ] 통합 테스트

### Phase 5: 자동화
- [ ] LawArticleUpdateScheduler 구현
- [ ] 법령 데이터 동기화 로직
- [ ] 배치 작업 설정
- [ ] 모니터링 설정

### Phase 6: 최적화
- [ ] 검색 품질 개선
- [ ] 성능 최적화
- [ ] 정확도 검증
- [ ] 사용자 피드백 수집

---

## 🚀 빠른 시작 가이드

### 1. 법령 조문 데이터 확인
```sql
-- 법령 조문 내용이 있는 테이블 확인
SELECT * FROM law_article WHERE law_id = '...';
```

### 2. 법령 조문 임베딩 서비스 구현
```java
@Service
@RequiredArgsConstructor
public class LawArticleEmbeddingService {
    private final LawArticleRepository lawArticleRepository;
    private final VectorStore vectorStore;
    
    @Transactional
    public int embedAllLawArticles() {
        List<LawArticle> articles = lawArticleRepository.findAll();
        // 임베딩 로직 구현
    }
}
```

### 3. 통합 검색 서비스 구현
```java
@Service
@RequiredArgsConstructor
public class UnifiedSearchService {
    private final ConsultationSearchService consultationSearchService;
    private final LawArticleSearchService lawArticleSearchService;
    
    public UnifiedSearchResult search(String query, int topK) {
        // 통합 검색 로직
    }
}
```

---

## 📚 참고 자료

- [한국 법령정보센터](https://www.law.go.kr/)
- [법제처 법령정보 API](https://www.law.go.kr/LSW/openapi/openApiGuide.do)
- [Spring AI Vector Store 문서](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- [RAG (Retrieval-Augmented Generation) 패턴](https://www.promptingguide.ai/techniques/rag)

---

**마지막 업데이트**: 2025-01-23
