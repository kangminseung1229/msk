-- ============================================
-- 벡터 임베딩 테이블 생성 쿼리
-- ============================================
-- 
-- 이 스크립트는 Spring AI PgVectorStore를 위한 벡터 임베딩 테이블을 생성합니다.
-- VectorStoreConfig에서 initializeSchema(false)로 설정되어 있으므로
-- 수동으로 테이블을 생성해야 합니다.
--
-- 사용 전 확인사항:
-- 1. PostgreSQL에 pgvector 확장이 설치되어 있어야 합니다.
-- 2. 데이터베이스에 pgvector 확장이 활성화되어 있어야 합니다.
-- 3. public 스키마가 존재해야 합니다 (기본적으로 존재함)
-- ============================================

-- 1. pgvector 확장 활성화 (필수)
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. public 스키마 확인 (이미 존재해야 함)
--    만약 public 스키마가 없다면 아래 주석을 해제하세요
-- CREATE SCHEMA IF NOT EXISTS public;

-- 3. Spring AI Vector Store 테이블 생성
--    Spring AI PgVectorStore의 기본 테이블 이름: vector_store
--    VectorStoreConfig에서 dimensions=768로 설정되어 있음
--    참고: metadata는 json 타입 (Spring AI 기본값)
--    
--    UUID 생성 방법:
--    - PostgreSQL 13+: gen_random_uuid() (기본 제공, 확장 불필요)
--    - PostgreSQL 12 이하: uuid_generate_v4() (uuid-ossp 확장 필요)
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- PostgreSQL 13+ 사용
    -- id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),  -- PostgreSQL 12 이하 사용 시 주석 해제
    content TEXT,
    metadata JSON,
    embedding vector(768) NOT NULL
);

-- 4. 벡터 유사도 검색을 위한 인덱스 생성 (HNSW 인덱스 - 빠른 검색)
--    COSINE_DISTANCE를 사용하는 경우 (VectorStoreConfig 기본값)
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx 
ON vector_store 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- 또는 IVFFlat 인덱스 (메모리 사용량이 적지만 HNSW보다 느림)
-- CREATE INDEX IF NOT EXISTS vector_store_embedding_idx 
-- ON vector_store 
-- USING ivfflat (embedding vector_cosine_ops)
-- WITH (lists = 100);

-- 5. 메타데이터 검색을 위한 GIN 인덱스 (선택사항, 성능 향상)
CREATE INDEX IF NOT EXISTS vector_store_metadata_idx 
ON vector_store 
USING GIN (metadata);

-- 6. 테이블 및 인덱스 확인
-- \d vector_store
-- \d+ vector_store_embedding_idx

-- ============================================
-- 테이블 구조 확인 쿼리
-- ============================================
-- SELECT 
--     column_name, 
--     data_type, 
--     character_maximum_length
-- FROM information_schema.columns 
-- WHERE table_name = 'vector_store'
-- ORDER BY ordinal_position;

-- ============================================
-- 테이블 삭제 (주의: 모든 벡터 데이터가 삭제됩니다)
-- ============================================
-- DROP TABLE IF EXISTS vector_store CASCADE;

-- ============================================
-- 데이터 확인 쿼리
-- ============================================
-- SELECT 
--     id,
--     LEFT(content, 100) as content_preview,
--     metadata
-- FROM vector_store
-- LIMIT 10;

-- ============================================
-- 벡터 유사도 검색 예시 쿼리
-- ============================================
-- -- 예시: 특정 벡터와 유사한 벡터 검색 (상위 5개)
-- SELECT 
--     id,
--     content,
--     metadata,
--     1 - (embedding <=> '[0.1, 0.2, 0.3, ...]'::vector) as similarity
-- FROM vector_store
-- ORDER BY embedding <=> '[0.1, 0.2, 0.3, ...]'::vector
-- LIMIT 5;
--
-- -- <=> 연산자는 COSINE_DISTANCE를 의미합니다.
-- -- similarity 값이 1에 가까울수록 유사합니다.
