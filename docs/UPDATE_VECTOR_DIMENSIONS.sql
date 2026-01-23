-- ============================================
-- 벡터 테이블 차원 업데이트 스크립트
-- ============================================
-- 
-- 임베딩 모델 변경 시 벡터 차원이 변경되면
-- 데이터베이스 테이블의 벡터 컬럼 차원도 업데이트해야 합니다.
--
-- 사용 시나리오:
-- - 768 차원 → 1536 차원으로 변경
-- - 768 차원 → 3072 차원으로 변경
-- - 기타 차원 변경
--
-- 주의사항:
-- 1. 이 스크립트는 기존 데이터를 모두 삭제합니다.
-- 2. 차원 변경 후에는 반드시 임베딩을 재생성해야 합니다.
-- 3. 백업을 먼저 수행하세요.
-- ============================================

-- 1. 기존 테이블 삭제 (모든 데이터가 삭제됩니다!)
--    주의: 이 작업은 되돌릴 수 없습니다. 백업을 먼저 수행하세요.
DROP TABLE IF EXISTS spring_ai_vector_store CASCADE;

-- 2. 새로운 차원으로 테이블 재생성
--    현재 문제: 모델이 3072 차원을 생성하는데, DB는 1536으로 설정되어 있음
--    
--    해결 방법 1: DB를 3072 차원으로 변경 (모델이 3072를 생성하는 경우)
--    해결 방법 2: 모델 설정을 수정하여 1536을 생성하도록 함
--
--    차원 옵션:
--    - 768: 빠른 검색 속도
--    - 1536: 성능과 비용의 균형 (권장)
--    - 3072: 최고 품질 (기본값, output-dimensionality 설정이 적용되지 않으면 이 값 사용)
--
--    주의: 모델이 실제로 생성하는 차원과 일치해야 합니다.
--    에러 메시지에서 "expected X dimensions, not Y"를 확인하여
--    실제 생성되는 차원(Y)에 맞춰 설정하세요.
--
--    현재 에러: "expected 1536 dimensions, not 3072"
--    → 모델이 3072를 생성하고 있으므로, DB를 3072로 변경하거나
--      모델 설정을 수정하여 1536을 생성하도록 해야 합니다.

-- 옵션 1: 3072 차원으로 설정 (모델이 3072를 생성하는 경우)
CREATE TABLE spring_ai_vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    metadata JSON,
    embedding vector(3072) NOT NULL  -- 모델이 3072를 생성하는 경우
);

-- 옵션 2: 1536 차원으로 설정 (output-dimensionality=1536이 적용되는 경우)
-- CREATE TABLE spring_ai_vector_store (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     content TEXT,
--     metadata JSON,
--     embedding vector(1536) NOT NULL  -- output-dimensionality=1536이 적용되는 경우
-- );

-- 3. 벡터 유사도 검색을 위한 인덱스 생성 (HNSW 인덱스)
CREATE INDEX spring_ai_vector_store_embedding_idx 
ON spring_ai_vector_store 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- 4. 메타데이터 검색을 위한 GIN 인덱스 (선택사항)
CREATE INDEX spring_ai_vector_store_metadata_idx 
ON spring_ai_vector_store 
USING GIN (metadata);

-- ============================================
-- 차원만 변경하고 데이터를 유지하려는 경우 (고급)
-- ============================================
-- 주의: 이 방법은 복잡하고 시간이 오래 걸릴 수 있습니다.
-- 대부분의 경우 테이블을 재생성하고 임베딩을 재생성하는 것이 더 안전합니다.
--
-- 1. 임시 테이블 생성 (새 차원)
-- CREATE TABLE spring_ai_vector_store_new (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     content TEXT,
--     metadata JSON,
--     embedding vector(1536) NOT NULL
-- );
--
-- 2. 기존 데이터를 새 테이블로 마이그레이션
--    (임베딩은 재생성해야 하므로 content와 metadata만 복사)
-- INSERT INTO spring_ai_vector_store_new (content, metadata)
-- SELECT content, metadata FROM spring_ai_vector_store;
--
-- 3. 기존 테이블 삭제 및 이름 변경
-- DROP TABLE spring_ai_vector_store CASCADE;
-- ALTER TABLE spring_ai_vector_store_new RENAME TO spring_ai_vector_store;
--
-- 4. 인덱스 재생성
-- CREATE INDEX spring_ai_vector_store_embedding_idx 
-- ON spring_ai_vector_store 
-- USING hnsw (embedding vector_cosine_ops)
-- WITH (m = 16, ef_construction = 64);

-- ============================================
-- 현재 테이블 차원 확인
-- ============================================
-- SELECT 
--     column_name, 
--     data_type,
--     udt_name
-- FROM information_schema.columns 
-- WHERE table_name = 'spring_ai_vector_store'
--   AND column_name = 'embedding';
