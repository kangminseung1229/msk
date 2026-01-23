package ai.langgraph4j.aiagent.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ai.langgraph4j.aiagent.entity.law.Article;
import ai.langgraph4j.aiagent.entity.law.LawBasicInformation;
import ai.langgraph4j.aiagent.metadata.LawArticleMetadata;
import ai.langgraph4j.aiagent.repository.ArticleRepository;
import ai.langgraph4j.aiagent.repository.LawBasicInformationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 법령 조문 임베딩 서비스
 * lawId별 최신 enforceDate의 법령 조문들을 임베딩하여 Vector Store에 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LawArticleEmbeddingService {

	private final LawBasicInformationRepository lawBasicInformationRepository;
	private final ArticleRepository articleRepository;
	private final VectorStore vectorStore;
	private final ApplicationContext applicationContext;

	/**
	 * 자기 자신의 프록시를 가져와서 트랜잭션이 적용된 메서드를 호출
	 */
	private LawArticleEmbeddingService getSelf() {
		return applicationContext.getBean(LawArticleEmbeddingService.class);
	}

	/**
	 * 모든 lawId별 최신 법령의 조문들을 임베딩하여 Vector Store에 저장
	 * 페이징 처리로 메모리 사용량을 최적화하고, 각 법령 처리 후 즉시 flush합니다.
	 * 각 법령 처리는 독립적인 트랜잭션으로 실행되어, 하나의 법령에서 오류가 발생해도
	 * 다른 법령 처리는 계속 진행됩니다.
	 * 
	 * @return 처리된 문서 수 (청크 포함)
	 */
	public int embedAllLatestLawArticles() {
		log.info("전체 최신 법령 조문 임베딩 시작 (페이징 처리 모드)");

		// 전체 법령 수 조회 (별도 트랜잭션)
		long totalCount = getSelf().countAllLatestLawArticles();
		log.info("전체 최신 법령 수: {}", totalCount);

		if (totalCount == 0) {
			log.warn("임베딩할 최신 법령이 없습니다");
			return 0;
		}

		// 배치 크기 설정 (메모리 사용량 최적화)
		int batchSize = 20; // 한 번에 처리할 법령 수
		int totalProcessed = 0;
		int totalArticles = 0;
		int page = 0;

		// 페이지네이션을 사용한 배치 처리
		while (true) {
			// 각 페이지 조회는 별도 트랜잭션으로 실행
			Page<LawBasicInformation> pageResult = getSelf().findAllLatestByLawIdPage(page, batchSize);

			if (pageResult.isEmpty()) {
				break;
			}

			List<LawBasicInformation> batch = pageResult.getContent();
			log.info("배치 처리 진행 중: {}/{} ({}%)",
					page * batchSize + batch.size(), totalCount,
					totalCount > 0 ? (int) ((page * batchSize + batch.size()) * 100.0 / totalCount) : 0);

			// 각 법령을 처리하고 즉시 flush (각 법령마다 독립적인 트랜잭션)
			for (LawBasicInformation law : batch) {
				try {
					// 각 법령 처리를 독립적인 트랜잭션으로 실행
					ProcessingResult result = getSelf().processLawArticleEmbedding(law);
					totalProcessed += result.processed;
					totalArticles += result.articles;

					log.debug("법령 ID {} (lawId: {}) 처리 완료: {}개 조문, {}개 문서 임베딩",
							law.getId(), law.getLawId(), result.articles, result.processed);

				} catch (Exception e) {
					log.error("법령 ID {} (lawId: {}) 처리 중 오류 발생", law.getId(), law.getLawId(), e);
					// 오류가 발생해도 다음 법령 처리는 계속 진행
				}
			}

			// 다음 페이지가 없으면 종료
			if (!pageResult.hasNext()) {
				break;
			}

			page++;

			// 중간 진행 상황 로깅
			if (page % 10 == 0) {
				log.info("중간 진행 상황: {}건 처리 완료", page * batchSize);
			}
		}

		log.info("전체 최신 법령 조문 임베딩 완료: 총 {}개 법령, {}개 조문, {}개 문서 임베딩",
				totalCount, totalArticles, totalProcessed);
		return totalProcessed;
	}

	/**
	 * 전체 최신 법령 수 조회 (읽기 전용 트랜잭션)
	 */
	@Transactional(readOnly = true)
	public long countAllLatestLawArticles() {
		return lawBasicInformationRepository.countAllLatestByLawId();
	}

	/**
	 * 최신 법령 목록 페이징 조회 (읽기 전용 트랜잭션)
	 */
	@Transactional(readOnly = true)
	public Page<LawBasicInformation> findAllLatestByLawIdPage(int page, int batchSize) {
		Pageable pageable = PageRequest.of(page, batchSize);
		return lawBasicInformationRepository.findAllLatestByLawId(pageable);
	}

	/**
	 * 법령 처리 결과를 담는 내부 클래스
	 */
	private static class ProcessingResult {
		final int processed;
		final int articles;

		ProcessingResult(int processed, int articles) {
			this.processed = processed;
			this.articles = articles;
		}
	}

	/**
	 * 개별 법령의 조문 임베딩 처리 (독립적인 트랜잭션)
	 * REQUIRES_NEW를 사용하여 부모 트랜잭션과 독립적으로 실행되므로,
	 * 오류가 발생해도 롤백되지 않고 다음 처리를 계속할 수 있습니다.
	 * readOnly = false로 설정하여 Vector Store에 INSERT가 가능하도록 합니다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ProcessingResult processLawArticleEmbedding(LawBasicInformation law) {
		// 조문 목록 조회 (삭제되지 않은 조문만)
		List<Article> articles = articleRepository.findActiveArticlesByLawBasicInformationId(law.getId());

		if (articles.isEmpty()) {
			log.debug("법령 ID {} (lawId: {})에 조문이 없습니다", law.getId(), law.getLawId());
			return new ProcessingResult(0, 0);
		}

		// 조문 임베딩 처리 (각 법령마다 즉시 flush)
		int processed = embedArticlesForLaw(law, articles);

		return new ProcessingResult(processed, articles.size());
	}

	/**
	 * 특정 lawId의 최신 법령 조문들을 임베딩하여 Vector Store에 저장
	 * readOnly = false로 설정하여 Vector Store에 INSERT가 가능하도록 합니다.
	 * 
	 * @param lawId 법령ID
	 * @return 처리된 조문 수
	 */
	@Transactional
	public int embedLatestLawArticlesByLawId(String lawId) {
		log.info("법령 ID {}의 최신 조문 임베딩 시작", lawId);

		// 1. lawId별 최신 법령 조회
		LawBasicInformation law = lawBasicInformationRepository.findLatestByLawId(lawId)
				.orElseThrow(() -> new IllegalArgumentException("법령을 찾을 수 없습니다: " + lawId));

		// 2. 조문 목록 조회 (삭제되지 않은 조문만)
		List<Article> articles = articleRepository.findActiveArticlesByLawBasicInformationId(law.getId());

		if (articles.isEmpty()) {
			log.warn("법령 ID {} (lawId: {})에 조문이 없습니다", law.getId(), lawId);
			return 0;
		}

		// 3. 조문 임베딩 처리
		int processed = embedArticlesForLaw(law, articles);

		log.info("법령 ID {} (lawId: {}) 임베딩 완료: {}개 조문, {}개 문서 임베딩",
				law.getId(), lawId, articles.size(), processed);
		return processed;
	}

	/**
	 * 특정 법령의 조문들을 임베딩하여 Vector Store에 저장
	 * 각 조문을 처리할 때마다 즉시 flush하여 메모리 사용량을 최적화합니다.
	 * 
	 * @param law      법령 기본 정보
	 * @param articles 조문 목록
	 * @return 처리된 문서 수 (청크 포함)
	 */
	private int embedArticlesForLaw(LawBasicInformation law, List<Article> articles) {
		int totalDocuments = 0;
		int successCount = 0;
		int failCount = 0;

		for (Article article : articles) {
			List<Document> documents = new ArrayList<>();

			try {
				// 1. 텍스트 준비
				String text = LawArticleMetadata.buildArticleText(law, article);

				if (text == null || text.trim().isEmpty()) {
					log.warn("조문 ID {}의 텍스트가 비어있어 건너뜁니다", article.getId());
					failCount++;
					continue;
				}

				// 2. 텍스트를 청크로 분할 (최대 토큰 수 제한: 2,048 토큰)
				// 대략적으로 1 토큰 = 4 문자로 계산, 안전하게 1,500 토큰 = 약 6,000 문자로 청크 분할
				List<String> chunks = splitTextIntoChunks(text, 6000);

				// 3. 각 청크를 Document로 생성
				for (int i = 0; i < chunks.size(); i++) {
					String chunk = chunks.get(i);

					// 메타데이터 준비
					LawArticleMetadata metadata = LawArticleMetadata.from(law, article);

					// 청크 정보 추가
					if (chunks.size() > 1) {
						metadata.setChunkIndex(i);
						metadata.setTotalChunks(chunks.size());
					}

					// Document 생성 (toMap()으로 변환)
					Document document = new Document(chunk, metadata.toMap());
					documents.add(document);
				}

				// 4. 각 조문의 문서를 즉시 Vector Store에 저장 (flush)
				if (!documents.isEmpty()) {
					try {
						vectorStore.add(documents);
						totalDocuments += documents.size();
						successCount++;
						log.debug("조문 ID {} 임베딩 완료 ({}개 청크)", article.getId(), chunks.size());
					} catch (Exception e) {
						log.error("조문 ID {} Vector Store 저장 중 오류 발생", article.getId(), e);
						failCount++;
					}
				}

			} catch (Exception e) {
				log.error("조문 ID {} 임베딩 중 오류 발생", article.getId(), e);
				failCount++;
			}
		}

		log.debug("법령 ID {} (lawId: {}) 처리 완료: 성공 {}건, 실패 {}건, 총 문서 {}개",
				law.getId(), law.getLawId(), successCount, failCount, totalDocuments);

		return totalDocuments;
	}

	/**
	 * 텍스트를 청크로 분할
	 * text-embedding-004 모델은 최대 2,048 토큰까지만 지원하므로
	 * 긴 텍스트를 적절한 크기로 나눕니다.
	 * 
	 * @param text         원본 텍스트
	 * @param maxChunkSize 최대 청크 크기 (문자 수, 약 1,500 토큰에 해당)
	 * @return 분할된 텍스트 청크 리스트
	 */
	private List<String> splitTextIntoChunks(String text, int maxChunkSize) {
		List<String> chunks = new ArrayList<>();

		if (text.length() <= maxChunkSize) {
			// 텍스트가 최대 크기보다 작으면 그대로 반환
			chunks.add(text);
			return chunks;
		}

		// 텍스트를 문장 단위로 분할하여 의미 있는 경계에서 나눔
		String[] sentences = text.split("(?<=[.!?。！？])\\s+");
		StringBuilder currentChunk = new StringBuilder();

		for (String sentence : sentences) {
			// 현재 청크에 문장을 추가했을 때 최대 크기를 초과하는지 확인
			if (currentChunk.length() + sentence.length() + 1 > maxChunkSize && currentChunk.length() > 0) {
				// 현재 청크를 저장하고 새 청크 시작
				chunks.add(currentChunk.toString().trim());
				currentChunk = new StringBuilder();
			}

			if (currentChunk.length() > 0) {
				currentChunk.append(" ");
			}
			currentChunk.append(sentence);
		}

		// 마지막 청크 추가
		if (currentChunk.length() > 0) {
			chunks.add(currentChunk.toString().trim());
		}

		// 문장 단위 분할이 실패한 경우 (문장 구분자가 없는 경우) 문자 단위로 분할
		if (chunks.isEmpty()) {
			for (int i = 0; i < text.length(); i += maxChunkSize) {
				int end = Math.min(i + maxChunkSize, text.length());
				chunks.add(text.substring(i, end));
			}
		}

		return chunks;
	}

	/**
	 * 특정 법령의 조문 임베딩을 삭제하고 재임베딩
	 * 
	 * @param lawId 법령ID
	 */
	@Transactional
	public void reembedLawArticlesByLawId(String lawId) {
		log.info("법령 ID {} 재임베딩 시작", lawId);

		// 1. 기존 임베딩 삭제 (메타데이터로 필터링)
		deleteEmbeddingsByLawId(lawId);

		// 2. 재임베딩
		embedLatestLawArticlesByLawId(lawId);

		log.info("법령 ID {} 재임베딩 완료", lawId);
	}

	/**
	 * 특정 lawId의 모든 조문 임베딩을 삭제
	 * PgVectorStore의 경우 메타데이터로 필터링하여 삭제합니다.
	 * 
	 * @param lawId 법령ID
	 */
	@Transactional
	public void deleteEmbeddingsByLawId(String lawId) {
		log.info("법령 ID {}의 임베딩 삭제 시작", lawId);

		// Spring AI VectorStore는 직접적인 삭제 메서드를 제공하지 않으므로
		// PgVectorStore의 경우 SQL을 직접 실행해야 합니다.
		log.warn("법령 ID {}의 임베딩 삭제는 SQL로 직접 실행 필요: " +
				"DELETE FROM spring_ai_vector_store WHERE metadata->>'lawId' = '{}' AND metadata->>'documentType' = 'lawArticle'",
				lawId, lawId);

		// TODO: JdbcTemplate을 주입받아서 SQL 실행하도록 구현
		// 또는 VectorStore 확장하여 deleteByMetadata 메서드 추가
	}
}
