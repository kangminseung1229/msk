# gemini-embedding-001 마이그레이션 가이드

## 변경 사항 요약

### 모델 변경
- **이전**: `text-embedding-004` (768 차원, deprecated 예정)
- **변경**: `gemini-embedding-001` (1536 차원, 최신 모델)

### 설정 변경
1. **모델명**: `text-embedding-004` → `gemini-embedding-001`
2. **출력 차원**: `output-dimensionality=1536` 추가
3. **pgvector 차원**: `768` → `1536`

### 선택한 차원: 1536
- **이유**: 성능과 비용의 균형점
- **장점**: 
  - 768 차원 대비 성능 향상
  - 3072 차원 대비 저장 공간 및 검색 속도 최적화
  - 실용적인 선택

---

## 마이그레이션 단계

### 1. 기존 벡터 DB 백업 (선택사항)
```sql
-- PostgreSQL에서 기존 벡터 테이블 백업
pg_dump -h localhost -U postgres -d consultation_db -t vector_store -F c -f vector_backup.dump
```

### 2. 기존 벡터 데이터 삭제
```sql
-- PostgreSQL에 연결
psql -h localhost -U postgres -d consultation_db

-- 기존 벡터 테이블 삭제
DROP TABLE IF EXISTS vector_store CASCADE;

-- 또는 데이터만 삭제 (테이블 구조 유지)
-- TRUNCATE TABLE vector_store;
```

### 3. pgvector 인덱스 재생성 (차원 변경)
```sql
-- 기존 인덱스가 있다면 삭제
DROP INDEX IF EXISTS vector_store_embedding_idx;

-- 1536 차원으로 인덱스 재생성 (Spring AI가 자동으로 생성하지만 수동으로도 가능)
-- Spring AI가 자동으로 처리하므로 보통 필요 없음
```

### 4. 애플리케이션 재시작
```bash
# 애플리케이션 재시작
./mvnw spring-boot:run
# 또는
./mvnw spring-boot:run
```

### 5. 벡터 데이터 재생성
애플리케이션이 시작되면 다음 API를 호출하여 벡터 데이터를 재생성합니다:

#### 상담 데이터 임베딩
```bash
# Swagger UI에서 호출하거나
curl -X POST http://localhost:8080/api/embedding/counsel/all
```

#### 법령 조문 임베딩
```bash
# Swagger UI에서 호출하거나
curl -X POST http://localhost:8080/api/embedding/law-article/all
```

---

## 변경된 설정 확인

### application.properties
```properties
# 모델 설정
spring.ai.google.genai.embedding.text.options.model=gemini-embedding-001
spring.ai.google.genai.embedding.text.options.output-dimensionality=1536

# pgvector 설정
spring.ai.vectorstore.pgvector.dimensions=1536
```

---

## 주의사항

### 1. 비용 증가
- **이전**: $0.10 / 100만 토큰
- **변경**: $0.15 / 100만 토큰 (50% 증가)
- **모니터링**: API 사용량 및 비용을 정기적으로 확인하세요

### 2. 저장 공간 증가
- **이전**: 768 차원 × 문서 수
- **변경**: 1536 차원 × 문서 수 (약 2배)
- **예상**: 1M 문서 기준 약 6GB (768 차원 대비 2배)

### 3. 검색 성능
- 1536 차원은 768 차원보다 검색 속도가 약간 느릴 수 있지만, HNSW 인덱스로 충분히 빠릅니다
- 성능 테스트를 통해 실제 검색 속도를 확인하세요

### 4. 호환성
- **기존 벡터와 호환되지 않음**: 반드시 재생성 필요
- **차원 불일치**: 모델 차원과 pgvector 차원이 일치해야 함

---

## 검증 체크리스트

- [ ] `application.properties` 설정 확인
- [ ] 기존 벡터 DB 삭제 완료
- [ ] 애플리케이션 재시작 성공
- [ ] 상담 데이터 임베딩 재생성 완료
- [ ] 법령 조문 임베딩 재생성 완료
- [ ] 벡터 검색 테스트 성공
- [ ] API 비용 모니터링 설정

---

## 롤백 방법

문제가 발생하면 다음 단계로 롤백할 수 있습니다:

1. `application.properties`를 이전 설정으로 복원
2. 기존 벡터 DB 백업에서 복원
3. 애플리케이션 재시작

```properties
# 롤백 설정
spring.ai.google.genai.embedding.text.options.model=text-embedding-004
# output-dimensionality 제거
spring.ai.vectorstore.pgvector.dimensions=768
```

---

## 성능 비교

### 예상 성능 향상
- **검색 정확도**: text-embedding-004 대비 향상 예상
- **다국어 지원**: 100+ 언어 지원 (이전 모델 대비 대폭 향상)
- **MTEB 벤치마크**: gemini-embedding-001이 우수한 성능

### 모니터링 포인트
- 벡터 검색 응답 시간
- 임베딩 생성 속도
- API 호출 비용
- 검색 결과 품질

---

## 문의 및 지원

문제가 발생하면 다음을 확인하세요:
1. 로그 파일 확인 (`logs/` 디렉토리)
2. PostgreSQL 연결 상태
3. API 키 유효성
4. 벡터 차원 일치 여부
