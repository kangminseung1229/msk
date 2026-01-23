package ai.langgraph4j.aiagent.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.langgraph4j.aiagent.entity.counsel.Counsel;
import ai.langgraph4j.aiagent.entity.law.LawArticleCode;
import ai.langgraph4j.aiagent.repository.CounselRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상담 데이터 임베딩 서비스
 * RDB의 counsel 테이블 데이터를 임베딩하여 Vector Store에 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationEmbeddingService {

	private final CounselRepository counselRepository;
	private final VectorStore vectorStore;

	/**
	 * 모든 상담 데이터를 임베딩하여 Vector Store에 저장
	 * 배치 처리로 메모리 사용량을 최적화합니다.
	 * 
	 * @return 처리된 상담 수
	 */
	@Transactional
	public int embedAllConsultations() {
		log.info("전체 상담 데이터 임베딩 시작 (배치 처리 모드)");

		// 전체 상담 수 조회
		long totalCount = counselRepository.count();
		log.info("전체 상담 수: {}", totalCount);

		if (totalCount == 0) {
			log.warn("임베딩할 상담이 없습니다");
			return 0;
		}

		// 배치 크기 설정 (메모리 사용량 최적화)
		int batchSize = 50; // 한 번에 처리할 상담 수
		int totalProcessed = 0;
		int page = 0;

		// 페이지네이션을 사용한 배치 처리
		while (true) {
			Pageable pageable = PageRequest.of(page, batchSize);
			Page<Counsel> pageResult = counselRepository.findAll(pageable);

			if (pageResult.isEmpty()) {
				break;
			}

			List<Counsel> batch = pageResult.getContent();
			log.info("배치 처리 진행 중: {}/{} ({}%)", totalProcessed, totalCount,
					totalCount > 0 ? (int) (totalProcessed * 100.0 / totalCount) : 0);

			// 배치 처리 및 즉시 저장
			int processed = embedConsultationsBatch(batch);
			totalProcessed += processed;

			// 다음 페이지가 없으면 종료
			if (!pageResult.hasNext()) {
				break;
			}

			page++;

			// 중간 진행 상황 로깅
			if (page % 10 == 0) {
				log.info("중간 진행 상황: {}건 처리 완료", totalProcessed);
			}
		}

		log.info("전체 임베딩 완료: 총 {}건 처리", totalProcessed);
		return totalProcessed;
	}

	/**
	 * 
	 * /**
	 * 특정 상담 ID의 데이터를 임베딩하여 Vector Store에 저장
	 * 
	 * @param consultationId 상담 ID
	 */
	@Transactional
	public void embedConsultation(Long consultationId) {
		log.info("상담 ID {} 임베딩 시작", consultationId);

		Counsel consultation = counselRepository.findById(consultationId)
				.orElseThrow(() -> new IllegalArgumentException("상담을 찾을 수 없습니다: " + consultationId));

		embedConsultations(List.of(consultation));
	}

	/**
	 * 상담 목록을 임베딩하여 Vector Store에 저장 (배치 처리용)
	 * 배치 단위로 처리하여 메모리 사용량을 최적화합니다.
	 * 
	 * @param consultations 상담 목록
	 * @return 처리된 상담 수
	 */
	private int embedConsultationsBatch(List<Counsel> consultations) {
		if (consultations.isEmpty()) {
			return 0;
		}

		List<Document> documents = new ArrayList<>();
		int successCount = 0;
		int failCount = 0;

		for (Counsel consultation : consultations) {
			try {
				// 1. 텍스트 준비 (제목 + 내용 + 답변)
				String text = buildText(consultation);

				if (text == null || text.trim().isEmpty()) {
					log.warn("상담 ID {}의 텍스트가 비어있어 건너뜁니다", consultation.getId());
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
					Map<String, Object> metadata = buildMetadata(consultation);

					// 청크 정보 추가
					if (chunks.size() > 1) {
						metadata.put("chunkIndex", i);
						metadata.put("totalChunks", chunks.size());
					}

					// Document 생성
					Document document = new Document(chunk, metadata);
					documents.add(document);
				}

				successCount++;
				log.debug("상담 ID {} 임베딩 준비 완료 ({}개 청크)", consultation.getId(), chunks.size());

			} catch (Exception e) {
				log.error("상담 ID {} 임베딩 중 오류 발생", consultation.getId(), e);
				failCount++;
			}
		}

		// 4. Vector Store에 즉시 저장 (배치 단위로 저장하여 메모리 해제)
		if (!documents.isEmpty()) {
			try {
				vectorStore.add(documents);
				log.debug("배치 저장 완료: 성공 {}건, 실패 {}건, 총 문서 {}개", successCount, failCount,
						documents.size());
			} catch (Exception e) {
				log.error("Vector Store 저장 중 오류 발생", e);
				throw new RuntimeException("Vector Store 저장 실패", e);
			}
		}

		return successCount;
	}

	/**
	 * 상담 목록을 임베딩하여 Vector Store에 저장 (단일 상담 처리용)
	 * 
	 * @param consultations 상담 목록
	 * @return 처리된 상담 수
	 */
	private int embedConsultations(List<Counsel> consultations) {
		return embedConsultationsBatch(consultations);
	}

	/**
	 * 상담 데이터로부터 임베딩할 텍스트 생성
	 * HTML 태그는 제거하고 순수 텍스트만 추출합니다.
	 * 
	 * @param consultation 상담 데이터
	 * @return 임베딩할 텍스트 (HTML 태그 제거됨)
	 */
	private String buildText(Counsel consultation) {
		StringBuilder text = new StringBuilder();

		// 제목 추가 (HTML 제거)
		if (consultation.getCounselTitle() != null && !consultation.getCounselTitle().trim().isEmpty()) {
			String cleanTitle = removeHtmlTags(consultation.getCounselTitle());
			if (!cleanTitle.trim().isEmpty()) {
				text.append("제목: ").append(cleanTitle).append("\n");
			}
		}

		// 내용 추가 (HTML 제거)
		if (consultation.getCounselContent() != null && !consultation.getCounselContent().trim().isEmpty()) {
			String cleanContent = removeHtmlTags(consultation.getCounselContent());
			if (!cleanContent.trim().isEmpty()) {
				text.append("내용: ").append(cleanContent).append("\n");
			}
		}

		// 답변 추가 (있는 경우, HTML 제거)
		if (consultation.getAnswerContent() != null && !consultation.getAnswerContent().trim().isEmpty()) {
			String cleanAnswer = removeHtmlTags(consultation.getAnswerContent());
			if (!cleanAnswer.trim().isEmpty()) {
				text.append("답변: ").append(cleanAnswer).append("\n");
			}
		}

		// 연관 법령 조문 추가 (lawArticleCodes)
		if (consultation.getLawArticleCodes() != null && !consultation.getLawArticleCodes().isEmpty()) {
			text.append("연관 법령: ");
			List<String> lawArticleStrings = consultation.getLawArticleCodes().stream()
					.map(lawCode -> {
						// 조문 키를 한국어 형식으로 변환 (예: "제1조", "제1조의2")
						return LawArticleCode.convertToKoreanFormat(lawCode.getArticleKey());
					})
					.toList();
			text.append(String.join(", ", lawArticleStrings));
		}

		return text.toString().trim();
	}

	/**
	 * HTML 태그를 제거하고 순수 텍스트만 추출
	 * JSoup을 사용하여 HTML을 파싱하고 모든 태그를 제거합니다.
	 * 
	 * @param html HTML이 포함된 텍스트
	 * @return HTML 태그가 제거된 순수 텍스트
	 */
	private String removeHtmlTags(String html) {
		if (html == null || html.trim().isEmpty()) {
			return "";
		}

		try {
			// JSoup을 사용하여 HTML 태그 제거
			// Safelist.none()은 모든 HTML 태그를 제거하고 텍스트만 추출
			String cleanText = Jsoup.clean(html, Safelist.none());

			// HTML 엔티티 디코딩 (&nbsp; -> 공백 등)
			cleanText = Jsoup.parse(cleanText).text();

			// 연속된 공백을 하나로 정리
			cleanText = cleanText.replaceAll("\\s+", " ").trim();

			return cleanText;
		} catch (Exception e) {
			log.warn("HTML 태그 제거 중 오류 발생, 원본 텍스트 반환: {}", e.getMessage());
			// 오류 발생 시 원본 반환 (최소한의 처리)
			return html.replaceAll("<[^>]+>", "").trim();
		}
	}

	/**
	 * 상담 데이터로부터 메타데이터 생성
	 * 
	 * @param consultation 상담 데이터
	 * @return 메타데이터
	 */
	private Map<String, Object> buildMetadata(Counsel consultation) {
		Map<String, Object> metadata = new HashMap<>();

		metadata.put("consultationId", consultation.getId());

		if (consultation.getCounselTitle() != null) {
			metadata.put("title", consultation.getCounselTitle());
		}

		if (consultation.getCounselFieldLarge() != null) {
			metadata.put("fieldLarge", consultation.getCounselFieldLarge().toString());
		}

		if (consultation.getCounselAt() != null) {
			metadata.put("createdAt", consultation.getCounselAt().toString());
		}

		// 연관 법령 조문 정보 추가
		if (consultation.getLawArticleCodes() != null && !consultation.getLawArticleCodes().isEmpty()) {
			List<String> lawArticleKeys = consultation.getLawArticleCodes().stream()
					.map(LawArticleCode::getArticleKey)
					.toList();
			metadata.put("lawArticleKeys", lawArticleKeys);
			
			// 한국어 형식으로도 저장 (검색 시 활용)
			List<String> lawArticleFormats = consultation.getLawArticleCodes().stream()
					.map(lawCode -> LawArticleCode.convertToKoreanFormat(lawCode.getArticleKey()))
					.toList();
			metadata.put("lawArticles", lawArticleFormats);
		}

		return metadata;
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
	 * 특정 상담의 임베딩을 삭제하고 재임베딩
	 * lawArticleCodes를 추가한 후 기존 임베딩을 업데이트할 때 사용합니다.
	 * 
	 * @param consultationId 상담 ID
	 */
	@Transactional
	public void reembedConsultation(Long consultationId) {
		log.info("상담 ID {} 재임베딩 시작", consultationId);
		
		// 1. 기존 임베딩 삭제 (메타데이터로 필터링)
		deleteEmbeddingsByConsultationId(consultationId);
		
		// 2. 재임베딩
		embedConsultation(consultationId);
		
		log.info("상담 ID {} 재임베딩 완료", consultationId);
	}

	/**
	 * 특정 상담 ID의 모든 임베딩을 삭제
	 * PgVectorStore의 경우 메타데이터로 필터링하여 삭제합니다.
	 * 
	 * @param consultationId 상담 ID
	 */
	@Transactional
	public void deleteEmbeddingsByConsultationId(Long consultationId) {
		log.info("상담 ID {}의 임베딩 삭제 시작", consultationId);
		
		// Spring AI VectorStore는 직접적인 삭제 메서드를 제공하지 않으므로
		// PgVectorStore의 경우 SQL을 직접 실행해야 합니다.
		// 하지만 일반적으로는 VectorStore 구현체에 따라 다릅니다.
		
		// 방법 1: VectorStore에 delete 메서드가 있는 경우
		// vectorStore.delete(List.of(consultationId.toString()));
		
		// 방법 2: SQL 직접 실행 (PgVectorStore의 경우)
		// JdbcTemplate을 주입받아서 사용하거나, 별도 서비스에서 처리
		log.warn("상담 ID {}의 임베딩 삭제는 SQL로 직접 실행 필요: " +
				"DELETE FROM spring_ai_vector_store WHERE metadata->>'consultationId' = '{}'",
				consultationId, consultationId);
		
		// TODO: JdbcTemplate을 주입받아서 SQL 실행하도록 구현
		// 또는 VectorStore 확장하여 deleteByMetadata 메서드 추가
	}

	/**
	 * 모든 상담 데이터를 재임베딩
	 * lawArticleCodes를 추가한 후 전체 재임베딩이 필요한 경우 사용합니다.
	 * 
	 * @return 처리된 상담 수
	 */
	@Transactional
	public int reembedAllConsultations() {
		log.warn("전체 상담 데이터 재임베딩 시작 (기존 임베딩 삭제 후 재생성)");
		
		// 1. 기존 임베딩 전체 삭제
		deleteAllEmbeddings();
		
		// 2. 전체 재임베딩
		return embedAllConsultations();
	}

	/**
	 * Vector Store의 모든 데이터 삭제 (주의: 개발/테스트용)
	 */
	@Transactional
	public void deleteAllEmbeddings() {
		log.warn("Vector Store의 모든 데이터 삭제 시작");
		// Spring AI Vector Store는 deleteAll 메서드를 제공하지 않으므로
		// 직접 SQL을 실행하거나 Vector Store 구현체에 따라 다릅니다.
		// PgVectorStore의 경우 직접 SQL 실행 필요
		log.warn("Vector Store 데이터 삭제는 수동으로 SQL 실행 필요: DELETE FROM spring_ai_vector_store;");
	}
}
