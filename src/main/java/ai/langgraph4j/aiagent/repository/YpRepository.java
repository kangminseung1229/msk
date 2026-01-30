package ai.langgraph4j.aiagent.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.langgraph4j.aiagent.entity.yp.Yp;

/**
 * 예규·판례(Yp) Repository
 */
@Repository
public interface YpRepository extends JpaRepository<Yp, Long> {

	/**
	 * 삭제되지 않은 예규판례를 ID 내림차순으로 페이징 조회
	 *
	 * @param pageable 페이지 정보
	 * @return 예규판례 페이지
	 */
	Page<Yp> findAllByDeleteYnFalseOrderByIdDesc(Pageable pageable);

	/**
	 * 삭제되지 않고, 문서일자(documentDate)가 기준일 이상인 예규판례를 ID 내림차순으로 페이징 조회.
	 * 옛날 판례를 제외하고 최근 판례만 임베딩/검색할 때 사용 (documentDate 형식: yyyy-MM-dd 또는 yyyyMMdd 등 문자열 비교 가능한 형식).
	 *
	 * @param fromDocumentDate 이상일 (이 날짜 이상만 포함, null이면 미포함)
	 * @param pageable         페이지 정보
	 * @return 예규판례 페이지
	 */
	Page<Yp> findAllByDeleteYnFalseAndDocumentDateGreaterThanEqualOrderByIdDesc(String fromDocumentDate, Pageable pageable);
}
