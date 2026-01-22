package ai.langgraph4j.msk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.langgraph4j.msk.entity.counsel.Counsel;

/**
 * 상담(Counsel) Repository
 * 다른 RDB의 counsel 테이블에서 데이터를 조회합니다.
 */
@Repository
public interface CounselRepository extends JpaRepository<Counsel, Long> {

	/**
	 * 페이지네이션을 사용한 상담 조회 (메모리 최적화)
	 * 
	 * @param pageable 페이지 정보
	 * @return 상담 페이지
	 */
	Page<Counsel> findAll(Pageable pageable);

}
