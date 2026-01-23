package ai.langgraph4j.aiagent.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import ai.langgraph4j.aiagent.entity.law.LawBasicInformation;
import ai.langgraph4j.aiagent.entity.law.QLawBasicInformation;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * LawBasicInformationRepository의 QueryDSL Extension 구현체
 */
@RequiredArgsConstructor
public class LawBasicInformationRepositoryExtensionImpl implements LawBasicInformationRepositoryExtension {

	private final JPAQueryFactory jpaQueryFactory;
	private final EntityManager entityManager;

	private QLawBasicInformation lawBasicInformation = QLawBasicInformation.lawBasicInformation;
	private QLawBasicInformation lawSub = new QLawBasicInformation("lawSub");

	@Override
	public Optional<LawBasicInformation> findLatestByLawId(String lawId) {

		// 서브쿼리: 해당 lawId의 최대 enforceDate 찾기
		JPQLQuery<String> maxEnforceDateSubquery = jpaQueryFactory
				.select(lawSub.enforceDate.max())
				.from(lawSub)
				.where(lawSub.lawId.eq(lawId));

		// 메인 쿼리: lawId가 일치하고 enforceDate가 최대값인 법령 조회
		LawBasicInformation result = jpaQueryFactory
				.selectFrom(lawBasicInformation)
				.where(lawBasicInformation.lawId.eq(lawId)
						.and(lawBasicInformation.enforceDate.eq(Expressions.stringTemplate("({0})", maxEnforceDateSubquery))))
				.fetchFirst();

		return Optional.ofNullable(result);
	}

	@Override
	public List<LawBasicInformation> findAllLatestByLawId() {
		// 상관 서브쿼리: 각 lawId별 최대 enforceDate 찾기
		// 외부 쿼리의 lawId와 매칭하여 해당 lawId의 최대 enforceDate를 찾음
		JPQLQuery<String> maxEnforceDateSubquery = jpaQueryFactory
				.select(lawSub.enforceDate.max())
				.from(lawSub)
				.where(lawSub.lawId.eq(lawBasicInformation.lawId));

		// 메인 쿼리: 각 lawId별로 enforceDate가 최대값인 법령 조회
		return jpaQueryFactory
				.selectFrom(lawBasicInformation)
				.where(lawBasicInformation.enforceDate.eq(Expressions.stringTemplate("({0})", maxEnforceDateSubquery)))
				.fetch();
	}

	@Override
	public Page<LawBasicInformation> findAllLatestByLawId(Pageable pageable) {
		// 상관 서브쿼리: 각 lawId별 최대 enforceDate 찾기
		JPQLQuery<String> maxEnforceDateSubquery = jpaQueryFactory
				.select(lawSub.enforceDate.max())
				.from(lawSub)
				.where(lawSub.lawId.eq(lawBasicInformation.lawId));

		// 메인 쿼리: 각 lawId별로 enforceDate가 최대값인 법령 조회
		JPQLQuery<LawBasicInformation> query = jpaQueryFactory
				.selectFrom(lawBasicInformation)
				.where(lawBasicInformation.enforceDate.eq(Expressions.stringTemplate("({0})", maxEnforceDateSubquery)));

		// 페이징 적용
		List<LawBasicInformation> content = query
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();

		// 총 개수 조회
		long total = countAllLatestByLawId();

		return new PageImpl<>(content, pageable, total);
	}

	@Override
	public long countAllLatestByLawId() {
		// INNER JOIN을 사용하여 각 lawId별로 enforceDate가 최대값인 법령 개수 조회
		// SQL: SELECT count(*) FROM law_basic_information l1 
		//      INNER JOIN (SELECT law_id, MAX(enforce_date) AS max_enforce_date FROM law_basic_information GROUP BY law_id) l2 
		//      ON l1.law_id = l2.law_id AND l1.enforce_date = l2.max_enforce_date
		String sql = "SELECT count(*) " +
				"FROM law_basic_information l1 " +
				"INNER JOIN (" +
				"    SELECT law_id, MAX(enforce_date) AS max_enforce_date " +
				"    FROM law_basic_information " +
				"    GROUP BY law_id" +
				") l2 ON l1.law_id = l2.law_id " +
				"    AND l1.enforce_date = l2.max_enforce_date";
		
		Object result = entityManager.createNativeQuery(sql).getSingleResult();
		return ((Number) result).longValue();
	}

	@Override
	public Set<String> findLawIdGroup() {
		// TODO Auto-generated method stub
		return null;
	}
}
