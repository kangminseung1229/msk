package ai.langgraph4j.aiagent.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.langgraph4j.aiagent.controller.dto.YpTokenEstimateResponse;
import ai.langgraph4j.aiagent.entity.yp.Yp;
import ai.langgraph4j.aiagent.metadata.YpMetadata;
import ai.langgraph4j.aiagent.repository.YpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 예규·판례(Yp) 임베딩 서비스.
 * 상위 N건(기본 100건) 예규판례에 대한 토큰 계산 및 임베딩을 제공합니다.
 * 옛날 판례는 유사도 검색 시 부적절할 수 있으므로, 기본적으로 최근 N년(기본 5년) 이내 문서일자만 대상으로 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YpEmbeddingService {

	private static final int DEFAULT_TOP_N = 100;
	/** 페이징 배치 크기 (한 번에 조회·임베딩할 예규판례 건수) */
	private static final int EMBED_PAGE_SIZE = 50;
	/** 임베딩/검색 대상으로 할 최근 연수 (이 기간 이내 문서일자만 포함, 옛날 판례 제외) */
	private static final int DEFAULT_RECENT_YEARS = 5;
	/** 한글 중심: 1 토큰 ≈ 3 문자 */
	private static final double TOKENS_PER_CHAR = 1.0 / 3.0;
	/** 약 2,000 토큰 (안전 마진 포함) */
	private static final int MAX_CHUNK_SIZE = 6000;

	private final YpRepository ypRepository;
	private final VectorStore vectorStore;

	/**
	 * 삭제되지 않은 예규판례 상위 100건에 대한 토큰 수·비용 예상 계산
	 *
	 * @return 토큰 계산 결과
	 */
	@Transactional(readOnly = true)
	public YpTokenEstimateResponse estimateTokensTop100() {
		return estimateTokensTopN(DEFAULT_TOP_N);
	}

	/**
	 * 삭제되지 않은 예규판례 상위 N건에 대한 토큰 수·비용 예상 계산
	 *
	 * @param topN 상위 N건 (1 이상)
	 * @return 토큰 계산 결과
	 */
	@Transactional
	public YpTokenEstimateResponse estimateTokensTopN(int topN) {
		if (topN < 1) {
			topN = DEFAULT_TOP_N;
		}
		String fromDate = getFromDocumentDate(DEFAULT_RECENT_YEARS);
		log.info("예규판례 상위 {}건 토큰 계산 시작 (문서일자 {} 이상)", topN, fromDate);

		Pageable pageable = PageRequest.of(0, topN);
		List<Yp> list = ypRepository.findAllByDeleteYnFalseAndDocumentDateGreaterThanEqualOrderByIdDesc(fromDate, pageable).getContent();

		if (list.isEmpty()) {
			log.warn("임베딩 대상 예규판례가 없습니다 (deleteYn=false, 문서일자 {} 이상, 상위 {}건)", fromDate, topN);
			return YpTokenEstimateResponse.builder()
					.totalCount(0)
					.embeddingModel("gemini-embedding-001")
					.costPerMillionTokens(new BigDecimal("0.15"))
					.totalChunks(0)
					.totalTokens(0L)
					.estimatedCost(BigDecimal.ZERO)
					.ypDetails(List.of())
					.build();
		}

		String embeddingModel = "gemini-embedding-001";
		BigDecimal costPerMillionTokens = new BigDecimal("0.15");
		BigDecimal costPerToken = costPerMillionTokens.divide(new BigDecimal("1000000"), 10, RoundingMode.HALF_UP);

		List<YpTokenEstimateResponse.YpTokenInfo> ypDetails = new ArrayList<>();
		long totalTokens = 0;
		int totalChunks = 0;

		for (Yp yp : list) {
			try {
				String text = YpMetadata.buildYpText(yp);
				if (text == null || text.trim().isEmpty()) {
					log.debug("예규판례 ID {} 텍스트 없음, 건너뜀", yp.getId());
					continue;
				}

				List<String> chunks = splitTextIntoChunks(text, MAX_CHUNK_SIZE);
				int estimatedChunks = chunks.size();

				long ypTokens = 0;
				for (String chunk : chunks) {
					ypTokens += Math.round(chunk.length() * TOKENS_PER_CHAR);
				}

				totalTokens += ypTokens;
				totalChunks += estimatedChunks;

				BigDecimal estimatedCost = costPerToken.multiply(new BigDecimal(ypTokens));

				log.info("[예규판례 토큰] ypId={}, documentNumber={}, title={}, textLength={}, estimatedTokens={}, estimatedChunks={}, estimatedCost=${}",
						yp.getId(), yp.getDocumentNumber(), yp.getTitle(), text.length(), ypTokens, estimatedChunks, estimatedCost);

				ypDetails.add(YpTokenEstimateResponse.YpTokenInfo.builder()
						.ypId(yp.getId())
						.documentNumber(yp.getDocumentNumber())
						.title(yp.getTitle())
						.textLength(text.length())
						.estimatedTokens(ypTokens)
						.estimatedChunks(estimatedChunks)
						.estimatedCost(estimatedCost)
						.build());
			} catch (Exception e) {
				log.error("예규판례 ID {} 토큰 계산 중 오류", yp.getId(), e);
			}
		}

		BigDecimal totalCost = costPerToken.multiply(new BigDecimal(totalTokens));

		log.info("예규판례 상위 {}건 토큰 계산 완료: {}건, {} 청크, {} 토큰, ${}",
				topN, ypDetails.size(), totalChunks, totalTokens, totalCost);

		return YpTokenEstimateResponse.builder()
				.totalCount(ypDetails.size())
				.embeddingModel(embeddingModel)
				.costPerMillionTokens(costPerMillionTokens)
				.totalChunks(totalChunks)
				.totalTokens(totalTokens)
				.estimatedCost(totalCost)
				.ypDetails(ypDetails)
				.build();
	}

	/**
	 * 텍스트를 청크로 분할 (embedding 모델 토큰 제한 고려)
	 */
	private List<String> splitTextIntoChunks(String text, int maxChunkSize) {
		List<String> chunks = new ArrayList<>();
		if (text.length() <= maxChunkSize) {
			chunks.add(text);
			return chunks;
		}

		String[] sentences = text.split("(?<=[.!?。！？])\\s+");
		StringBuilder currentChunk = new StringBuilder();

		for (String sentence : sentences) {
			if (currentChunk.length() + sentence.length() + 1 > maxChunkSize && !currentChunk.isEmpty()) {
				chunks.add(currentChunk.toString().trim());
				currentChunk = new StringBuilder();
			}
			if (!currentChunk.isEmpty()) {
				currentChunk.append(" ");
			}
			currentChunk.append(sentence);
		}
		if (!currentChunk.isEmpty()) {
			chunks.add(currentChunk.toString().trim());
		}
		if (chunks.isEmpty()) {
			for (int i = 0; i < text.length(); i += maxChunkSize) {
				int end = Math.min(i + maxChunkSize, text.length());
				chunks.add(text.substring(i, end));
			}
		}
		return chunks;
	}

	/** document_date 저장 형식 (yyyyMMdd) */
	private static final DateTimeFormatter DOCUMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	/** 문서일자 기준 'N년 전 오늘' 문자열 (yyyyMMdd). document_date 필드와 문자열 비교용. */
	private static String getFromDocumentDate(int recentYears) {
		if (recentYears <= 0) {
			recentYears = 1;
		}
		return LocalDate.now().minusYears(recentYears).format(DOCUMENT_DATE_FORMAT);
	}

	/**
	 * 최근 N년 이내 문서일자(document_date)인 예규판례를 페이징으로 전부 임베딩하여 Vector Store에 저장.
	 * 배치마다 Vector Store에 즉시 반영되도록 @Transactional을 걸지 않음 (루프 끝날 때까지 커밋 대기 방지).
	 *
	 * @param recentYears 최근 몇 년 이내 (예: 5 → 오늘 기준 5년 이내 document_date만 대상)
	 * @return 저장된 문서 수 (청크 포함)
	 */
	public int embedByRecentYears(int recentYears) {
		if (recentYears <= 0) {
			recentYears = DEFAULT_RECENT_YEARS;
		}
		String fromDate = getFromDocumentDate(recentYears);
		log.info("예규판례 임베딩 시작 (최근 {}년, document_date {} 이상, 페이징 크기 {})", recentYears, fromDate, EMBED_PAGE_SIZE);

		int totalDocuments = 0;
		int pageNumber = 0;

		while (true) {
			Pageable pageable = PageRequest.of(pageNumber, EMBED_PAGE_SIZE);
			Page<Yp> page = ypRepository.findAllByDeleteYnFalseAndDocumentDateGreaterThanEqualOrderByIdDesc(fromDate, pageable);

			if (page.isEmpty()) {
				break;
			}

			List<Yp> list = page.getContent();
			List<Document> documents = new ArrayList<>();

			for (Yp yp : list) {
				try {
					String text = YpMetadata.buildYpText(yp);
					if (text == null || text.trim().isEmpty()) {
						log.debug("예규판례 ID {} 텍스트 없음, 건너뜀", yp.getId());
						continue;
					}
					List<String> chunks = splitTextIntoChunks(text, MAX_CHUNK_SIZE);
					for (int i = 0; i < chunks.size(); i++) {
						YpMetadata metadata = YpMetadata.from(yp);
						if (chunks.size() > 1) {
							metadata.setChunkIndex(i);
							metadata.setTotalChunks(chunks.size());
						}
						documents.add(new Document(chunks.get(i), metadata.toMap()));
					}
					log.info("[예규판례 임베딩] ypId={}, documentNumber={}, title={}, textLength={}, chunks={}",
							yp.getId(), yp.getDocumentNumber(), yp.getTitle(), text.length(), chunks.size());
				} catch (Exception e) {
					log.error("예규판례 ID {} 임베딩 준비 중 오류", yp.getId(), e);
				}
			}

			if (!documents.isEmpty()) {
				vectorStore.add(documents);
				totalDocuments += documents.size();
			}

			log.info("예규판례 임베딩 진행: {}페이지 완료, 누적 {}개 문서", pageNumber + 1, totalDocuments);

			if (!page.hasNext()) {
				break;
			}
			pageNumber++;
		}

		log.info("예규판례 임베딩 완료: 최근 {}년, 총 {}개 문서", recentYears, totalDocuments);
		return totalDocuments;
	}

	/**
	 * 단일 예규판례 임베딩
	 *
	 * @param ypId 예규판례 ID
	 */
	@Transactional
	public void embedOne(Long ypId) {
		Yp yp = ypRepository.findById(ypId)
				.orElseThrow(() -> new IllegalArgumentException("예규판례를 찾을 수 없습니다: " + ypId));
		if (yp.isDeleteYn()) {
			throw new IllegalArgumentException("삭제된 예규판례입니다: " + ypId);
		}

		String text = YpMetadata.buildYpText(yp);
		if (text == null || text.trim().isEmpty()) {
			log.warn("예규판례 ID {} 텍스트 없음", ypId);
			return;
		}

		List<String> chunks = splitTextIntoChunks(text, MAX_CHUNK_SIZE);
		long ypTokens = 0;
		for (String chunk : chunks) {
			ypTokens += Math.round(chunk.length() * TOKENS_PER_CHAR);
		}
		List<Document> documents = new ArrayList<>();
		for (int i = 0; i < chunks.size(); i++) {
			YpMetadata metadata = YpMetadata.from(yp);
			if (chunks.size() > 1) {
				metadata.setChunkIndex(i);
				metadata.setTotalChunks(chunks.size());
			}
			documents.add(new Document(chunks.get(i), metadata.toMap()));
		}
		log.info("[예규판례 임베딩] ypId={}, documentNumber={}, title={}, textLength={}, estimatedTokens={}, chunks={}",
				ypId, yp.getDocumentNumber(), yp.getTitle(), text.length(), ypTokens, chunks.size());
		vectorStore.add(documents);
		log.info("예규판례 ID {} 임베딩 완료: {}개 청크", ypId, documents.size());
	}
}
