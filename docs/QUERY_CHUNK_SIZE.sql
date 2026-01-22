-- ============================================
-- 청크 크기별 조회 쿼리
-- ============================================
-- spring_ai_vector_store 테이블에서 청크 크기(문자 수)를 기준으로 내림차순 조회

-- 1. 기본 조회: 청크 크기(문자 수) 내림차순
--    content 컬럼의 길이를 청크 크기로 사용
SELECT 
    id,
    LEFT(content, 100) as content_preview,
    LENGTH(content) as chunk_size,
    metadata,
    metadata->>'consultationId' as consultation_id,
    metadata->>'title' as title,
    metadata->>'chunkIndex' as chunk_index,
    metadata->>'totalChunks' as total_chunks
FROM spring_ai_vector_store
ORDER BY LENGTH(content) DESC;

-- 2. 상위 N개만 조회 (예: 상위 10개)
SELECT 
    id,
    LEFT(content, 100) as content_preview,
    LENGTH(content) as chunk_size,
    metadata->>'consultationId' as consultation_id,
    metadata->>'title' as title,
    metadata->>'chunkIndex' as chunk_index,
    metadata->>'totalChunks' as total_chunks
FROM spring_ai_vector_store
ORDER BY LENGTH(content) DESC
LIMIT 10;

-- 3. 특정 청크 크기 범위 조회 (예: 5000~6000 문자)
SELECT 
    id,
    LEFT(content, 100) as content_preview,
    LENGTH(content) as chunk_size,
    metadata->>'consultationId' as consultation_id,
    metadata->>'title' as title,
    metadata->>'chunkIndex' as chunk_index,
    metadata->>'totalChunks' as total_chunks
FROM spring_ai_vector_store
WHERE LENGTH(content) BETWEEN 5000 AND 6000
ORDER BY LENGTH(content) DESC;

-- 4. 청크 크기별 통계 조회
SELECT 
    LENGTH(content) as chunk_size,
    COUNT(*) as count,
    MIN(LENGTH(content)) as min_size,
    MAX(LENGTH(content)) as max_size,
    AVG(LENGTH(content))::INTEGER as avg_size
FROM spring_ai_vector_store
GROUP BY LENGTH(content)
ORDER BY chunk_size DESC;

-- 5. 특정 상담 ID의 청크들을 크기별로 조회
SELECT 
    id,
    LEFT(content, 100) as content_preview,
    LENGTH(content) as chunk_size,
    metadata->>'chunkIndex' as chunk_index,
    metadata->>'totalChunks' as total_chunks
FROM spring_ai_vector_store
WHERE metadata->>'consultationId' = '37285'
ORDER BY LENGTH(content) DESC;

-- 6. 메타데이터에 chunkSize가 저장되어 있는 경우 (JSON 필드 사용)
--    만약 metadata JSON에 chunkSize 필드가 있다면:
SELECT 
    id,
    LEFT(content, 100) as content_preview,
    (metadata->>'chunkSize')::INTEGER as chunk_size,
    metadata->>'consultationId' as consultation_id,
    metadata->>'title' as title
FROM spring_ai_vector_store
WHERE metadata->>'chunkSize' IS NOT NULL
ORDER BY (metadata->>'chunkSize')::INTEGER DESC;

-- 7. 청크 크기별 분포 확인 (히스토그램)
SELECT 
    CASE 
        WHEN LENGTH(content) < 1000 THEN '0-1000'
        WHEN LENGTH(content) < 2000 THEN '1000-2000'
        WHEN LENGTH(content) < 3000 THEN '2000-3000'
        WHEN LENGTH(content) < 4000 THEN '3000-4000'
        WHEN LENGTH(content) < 5000 THEN '4000-5000'
        WHEN LENGTH(content) < 6000 THEN '5000-6000'
        ELSE '6000+'
    END as size_range,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM spring_ai_vector_store
GROUP BY size_range
ORDER BY MIN(LENGTH(content)) DESC;
