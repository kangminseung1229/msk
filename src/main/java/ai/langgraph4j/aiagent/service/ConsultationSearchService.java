package ai.langgraph4j.aiagent.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.langgraph4j.aiagent.config.PromptConfig;
import ai.langgraph4j.aiagent.service.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상담 데이터 벡터 검색 서비스
 * Vector Store에서 유사도 검색을 수행하고 결과를 반환합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationSearchService {

	private final VectorStore vectorStore;
	private final PromptConfig promptConfig;

	/**
	 * 벡터 유사도 검색 수행
	 * 
	 * @param query 검색 쿼리
	 * @param topK  반환할 최대 결과 수 (기본값: 5)
	 * @return 검색 결과 리스트
	 */
	@Transactional(readOnly = true)
	public List<SearchResult> search(String query, int topK) {
		log.info("벡터 검색 시작 - query: {}, topK: {}", query, topK);

		if (query == null || query.trim().isEmpty()) {
			log.warn("검색 쿼리가 비어있습니다");
			return new ArrayList<>();
		}

		try {
			// Vector Store에서 유사도 검색 수행
			SearchRequest searchRequest = SearchRequest.builder()
					.query(query)
					.topK(topK)
					.similarityThreshold(0.0) // 유사도 임계값 (0.0 ~ 1.0)
					.build();

			List<Document> documents = vectorStore.similaritySearch(searchRequest);

			log.info("검색 완료 - 결과 수: {}", documents.size());

			// Document를 SearchResult로 변환
			return documents.stream()
					.map(this::convertToSearchResult)
					.toList();

		} catch (Exception e) {
			log.error("벡터 검색 중 오류 발생 - query: {}", query, e);
			throw new SearchException("검색 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	/**
	 * 벡터 유사도 검색 수행 (기본 topK: 5)
	 * 
	 * @param query 검색 쿼리
	 * @return 검색 결과 리스트
	 */
	@Transactional(readOnly = true)
	public List<SearchResult> search(String query) {
		return search(query, 5);
	}

	/**
	 * 벡터 유사도 검색 수행 (유사도 임계값 지정)
	 * 
	 * @param query               검색 쿼리
	 * @param topK                반환할 최대 결과 수
	 * @param similarityThreshold 유사도 임계값 (0.0 ~ 1.0, 높을수록 더 유사한 결과만 반환)
	 * @return 검색 결과 리스트
	 */
	@Transactional(readOnly = true)
	public List<SearchResult> search(String query, int topK, double similarityThreshold) {
		log.info("벡터 검색 시작 - query: {}, topK: {}, threshold: {}", query, topK, similarityThreshold);

		if (query == null || query.trim().isEmpty()) {
			log.warn("검색 쿼리가 비어있습니다");
			return new ArrayList<>();
		}

		try {
			// Vector Store에서 유사도 검색 수행
			SearchRequest searchRequest = SearchRequest.builder()
					.query(query)
					.topK(topK)
					.similarityThreshold(similarityThreshold)
					.build();

			List<Document> documents = vectorStore.similaritySearch(searchRequest);

			log.info("검색 완료 - 결과 수: {}", documents.size());

			// 검색 결과가 없을 때 경고 로그
			if (documents.isEmpty()) {
				log.warn("검색 결과가 없습니다. similarityThreshold({})가 너무 높거나, 벡터 스토어에 데이터가 없을 수 있습니다.",
						similarityThreshold);
			}

			// 검색 결과 상세 로깅 (디버깅용)
			for (int i = 0; i < documents.size(); i++) {
				Document doc = documents.get(i);
				Map<String, Object> meta = doc.getMetadata();
				Double score = doc.getScore();
				String content = doc.getText();
				Long counselId = extractLong(meta, "counselId");
				String title = extractString(meta, "title");

				log.info("검색 결과 [{}] - counselId: {}, title: {}, score: {}, content: {}...",
						i + 1, counselId, title, score,
						content != null && content.length() > 100 ? content.substring(0, 100) : content);

				// 검색 쿼리와 결과의 관련성 체크 (간단한 키워드 매칭)
				if (query != null && title != null && content != null) {
					String queryLower = query.toLowerCase();
					String titleLower = title.toLowerCase();
					String contentLower = content.toLowerCase();

					boolean hasKeyword = titleLower.contains(queryLower) || contentLower.contains(queryLower);
					if (!hasKeyword && score != null && score > 0.8) {
						log.warn("⚠️ 높은 유사도({})이지만 검색 쿼리 '{}'와 결과의 제목/내용에 키워드가 없습니다. " +
								"임베딩 품질 문제일 수 있습니다.", score, query);
					}
				}
			}

			// Document를 SearchResult로 변환
			return documents.stream()
					.map(this::convertToSearchResult)
					.toList();

		} catch (Exception e) {
			log.error("벡터 검색 중 오류 발생 - query: {}", query, e);
			throw new SearchException("검색 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	/**
	 * 하이브리드 검색: 상담 데이터와 법령 데이터를 병렬로 검색하고, 상담 결과의 lawArticles로 추가 검색
	 * 
	 * @param query               검색 쿼리
	 * @param counselTopK         상담 데이터 검색 결과 수
	 * @param lawArticleTopK      법령 데이터 검색 결과 수
	 * @param similarityThreshold 유사도 임계값
	 * @return 통합 검색 결과 리스트
	 */
	@Transactional(readOnly = true)
	public List<SearchResult> hybridSearch(String query, int counselTopK, int lawArticleTopK,
			double similarityThreshold) {
		return hybridSearch(query, counselTopK, lawArticleTopK, similarityThreshold, null);
	}

	/**
	 * 하이브리드 검색: 상담 데이터와 법령 데이터를 병렬로 검색하고, 상담 결과의 lawArticles로 추가 검색.
	 * systemInstruction이 있으면 검색 쿼리와 결합하여 벡터 검색에 반영합니다.
	 * 
	 * @param query               검색 쿼리
	 * @param counselTopK         상담 데이터 검색 결과 수
	 * @param lawArticleTopK      법령 데이터 검색 결과 수
	 * @param similarityThreshold 유사도 임계값
	 * @param systemInstruction   검색 맥락 보강용 (선택, null 가능)
	 * @return 통합 검색 결과 리스트
	 */
	@Transactional(readOnly = true)
	public List<SearchResult> hybridSearch(String query, int counselTopK, int lawArticleTopK,
			double similarityThreshold, String systemInstruction) {
		String effectiveQuery = buildSearchQuery(query, systemInstruction);
		log.info(
				"하이브리드 검색 시작 - query: {}, systemInstruction 적용: {}, counselTopK: {}, lawArticleTopK: {}, threshold: {}",
				query, systemInstruction != null && !systemInstruction.isBlank(), counselTopK, lawArticleTopK,
				similarityThreshold);

		if (query == null || query.trim().isEmpty()) {
			log.warn("검색 쿼리가 비어있습니다");
			return new ArrayList<>();
		}

		try {
			// 1단계: 상담 데이터 검색 (documentType == 'counsel')
			SearchRequest counselSearchRequest = SearchRequest.builder()
					.query(effectiveQuery)
					.topK(counselTopK)
					.similarityThreshold(similarityThreshold)
					.filterExpression("documentType == 'counsel'")
					.build();

			List<Document> counselDocuments = vectorStore.similaritySearch(counselSearchRequest);
			log.info("상담 데이터 검색 완료 - 결과 수: {}", counselDocuments.size());

			// 2단계: 법령 데이터 검색 (documentType == 'lawArticle')
			// 법령 데이터는 상담 데이터보다 유사도 점수가 낮을 수 있으므로 임계값을 낮춤
			double lawArticleThreshold = Math.max(0.0, similarityThreshold - 0.1); // 최소 0.1 낮춤
			SearchRequest lawArticleSearchRequest = SearchRequest.builder()
					.query(effectiveQuery)
					.topK(lawArticleTopK)
					.similarityThreshold(lawArticleThreshold)
					.filterExpression("documentType == 'lawArticle'")
					.build();

			List<Document> lawArticleDocuments = vectorStore.similaritySearch(lawArticleSearchRequest);
			log.info("법령 데이터 검색 완료 - 결과 수: {} (임계값: {})", lawArticleDocuments.size(), lawArticleThreshold);

			// 검색 결과가 없을 때 디버깅 정보 출력
			if (lawArticleDocuments.isEmpty()) {
				log.warn("법령 데이터 검색 결과가 없습니다. " +
						"벡터 스토어에 법령 데이터가 없거나, 유사도 임계값({})이 너무 높을 수 있습니다.", lawArticleThreshold);

				// 필터 없이 검색해보기 (디버깅용)
				try {
					SearchRequest debugRequest = SearchRequest.builder()
							.query(effectiveQuery)
							.topK(5)
							.similarityThreshold(0.0)
							.filterExpression("documentType == 'lawArticle'")
							.build();
					List<Document> debugResults = vectorStore.similaritySearch(debugRequest);
					log.info("디버깅: 필터만 적용한 법령 데이터 검색 결과: {}건", debugResults.size());
					if (!debugResults.isEmpty()) {
						log.info("디버깅: 첫 번째 결과 유사도 점수: {}", debugResults.get(0).getScore());
					}
				} catch (Exception e) {
					log.debug("디버깅 검색 중 오류 (무시): {}", e.getMessage());
				}
			}

			// 3단계: 상담 결과에서 lawArticles 추출하여 추가 법령 검색
			Set<String> foundLawArticlePairs = new HashSet<>();
			for (Document counselDoc : counselDocuments) {
				Map<String, Object> metadata = counselDoc.getMetadata();
				@SuppressWarnings("unchecked")
				List<String> lawArticlePairs = (List<String>) metadata.get("lawArticlePairs");
				if (lawArticlePairs != null) {
					foundLawArticlePairs.addAll(lawArticlePairs);
				}
			}

			List<Document> relatedLawArticleDocuments = new ArrayList<>();
			if (!foundLawArticlePairs.isEmpty()) {
				log.info("상담 결과에서 추출한 lawArticlePairs: {}개", foundLawArticlePairs.size());
				// 연관 법령 조문 검색 시에도 임계값을 낮춤
				double relatedLawArticleThreshold = Math.max(0.0, similarityThreshold - 0.1);
				relatedLawArticleDocuments = searchLawArticlesByPairs(effectiveQuery, foundLawArticlePairs,
						lawArticleTopK,
						relatedLawArticleThreshold);
				log.info("연관 법령 조문 검색 완료 - 결과 수: {} (임계값: {})",
						relatedLawArticleDocuments.size(), relatedLawArticleThreshold);

				// 검색 결과가 없을 때 디버깅 정보 출력
				if (relatedLawArticleDocuments.isEmpty()) {
					log.warn("연관 법령 조문 검색 결과가 없습니다. " +
							"lawArticlePairs: {}, 임계값: {}", foundLawArticlePairs, relatedLawArticleThreshold);
				}
			}

			// 4단계: 결과 통합 및 중복 제거 (lawId:articleKey 기준)
			Map<String, SearchResult> resultMap = new LinkedHashMap<>();

			// 상담 결과 추가
			for (Document doc : counselDocuments) {
				SearchResult result = convertToSearchResult(doc);
				resultMap.put("counsel:" + result.getCounselId(), result);
			}

			// 법령 결과 추가 (중복 제거)
			int addedLawArticles = 0;
			for (Document doc : lawArticleDocuments) {
				Map<String, Object> meta = doc.getMetadata();
				String lawId = extractString(meta, "lawId");
				String articleKey = extractString(meta, "articleKey");
				String documentType = extractString(meta, "documentType");
				String key = "lawArticle:" + lawId + ":" + articleKey;
				log.debug("법령 검색 결과 처리 - lawId: {}, articleKey: {}, documentType: {}, key: {}", 
						lawId, articleKey, documentType, key);
				if (!resultMap.containsKey(key)) {
					SearchResult result = convertLawArticleToSearchResult(doc);
					log.debug("법령 결과 추가 - documentType: {}, title: {}", 
							result.getDocumentType(), result.getTitle());
					resultMap.put(key, result);
					addedLawArticles++;
				} else {
					log.debug("법령 결과 중복 제거됨 - key: {}", key);
				}
			}
			log.info("법령 검색 결과 추가 완료 - {}건 추가됨 (전체 법령 검색 결과: {}건)", 
					addedLawArticles, lawArticleDocuments.size());

			// 연관 법령 결과 추가 (중복 제거)
			int addedRelatedLawArticles = 0;
			for (Document doc : relatedLawArticleDocuments) {
				Map<String, Object> meta = doc.getMetadata();
				String lawId = extractString(meta, "lawId");
				String articleKey = extractString(meta, "articleKey");
				String documentType = extractString(meta, "documentType");
				String key = "lawArticle:" + lawId + ":" + articleKey;
				log.debug("연관 법령 검색 결과 처리 - lawId: {}, articleKey: {}, documentType: {}, key: {}", 
						lawId, articleKey, documentType, key);
				if (!resultMap.containsKey(key)) {
					SearchResult result = convertLawArticleToSearchResult(doc);
					log.debug("연관 법령 결과 추가 - documentType: {}, title: {}", 
							result.getDocumentType(), result.getTitle());
					resultMap.put(key, result);
					addedRelatedLawArticles++;
				} else {
					log.debug("연관 법령 결과 중복 제거됨 - key: {}", key);
				}
			}
			log.info("연관 법령 검색 결과 추가 완료 - {}건 추가됨 (전체 연관 법령 검색 결과: {}건)", 
					addedRelatedLawArticles, relatedLawArticleDocuments.size());

			List<SearchResult> results = new ArrayList<>(resultMap.values());
			
			// 최종 결과 분류 확인
			long finalCounselCount = results.stream()
					.filter(r -> "counsel".equals(r.getDocumentType()))
					.count();
			long finalLawArticleCount = results.stream()
					.filter(r -> "lawArticle".equals(r.getDocumentType()))
					.count();
			log.info("하이브리드 검색 완료 - 통합 결과 수: {} (상담: {}건, 법령: {}건)", 
					results.size(), finalCounselCount, finalLawArticleCount);

			return results;

		} catch (Exception e) {
			log.error("하이브리드 검색 중 오류 발생 - query: {}", query, e);
			throw new SearchException("하이브리드 검색 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	/**
	 * 검색 쿼리와 systemInstruction을 결합하여 벡터 검색용 쿼리를 만듭니다.
	 * systemInstruction이 null이거나 공백이면 query만 사용합니다.
	 * 보강 문구는 {@link PromptConfig#getSearchQueryContextPrefix()}에서 로드합니다.
	 */
	private String buildSearchQuery(String query, String systemInstruction) {
		if (systemInstruction == null || systemInstruction.isBlank()) {
			return query;
		}
		String prefix = promptConfig.getSearchQueryContextPrefix();
		return query + "\n\n" + prefix + " " + systemInstruction.trim();
	}

	/**
	 * lawArticlePairs를 기반으로 법령 조문 검색 (벡터 유사도 검색)
	 * 
	 * @param query               검색 쿼리
	 * @param lawArticlePairs     검색할 lawArticlePairs (예: ["lawId:articleKey"])
	 * @param topK                반환할 최대 결과 수
	 * @param similarityThreshold 유사도 임계값
	 * @return 검색된 법령 조문 문서 리스트
	 */
	private List<Document> searchLawArticlesByPairs(String query, Set<String> lawArticlePairs, int topK,
			double similarityThreshold) {
		List<Document> allResults = new ArrayList<>();

		// 각 lawArticlePair에 대해 벡터 유사도 검색 수행
		for (String pair : lawArticlePairs) {
			if (pair == null || !pair.contains(":")) {
				continue;
			}

			String[] parts = pair.split(":", 2);
			if (parts.length != 2) {
				continue;
			}

			String lawId = parts[0].trim();
			String articleKey = parts[1].trim();

			if (lawId.isEmpty() || articleKey.isEmpty()) {
				continue;
			}

			try {
				// 벡터 유사도 검색 (메타데이터 필터와 함께)
				// 메타데이터 필터로 정확히 매칭되는 법령 조문을 찾는 것이므로,
				// 유사도 임계값을 낮춰서 필터 조건만으로도 결과가 나오도록 함
				double effectiveThreshold = Math.max(0.0, similarityThreshold - 0.2);
				SearchRequest searchRequest = SearchRequest.builder()
						.query(query) // 실제 검색 쿼리 사용
						.topK(topK)
						.similarityThreshold(effectiveThreshold)
						.filterExpression(
								String.format("documentType == 'lawArticle' && lawId == '%s' && articleKey == '%s'",
										lawId, articleKey))
						.build();

				log.debug("연관 법령 조문 검색 - lawId: {}, articleKey: {}, 임계값: {} -> {}", 
						lawId, articleKey, similarityThreshold, effectiveThreshold);
				List<Document> documents = vectorStore.similaritySearch(searchRequest);
				log.debug("연관 법령 조문 검색 결과 - lawId: {}, articleKey: {}, 결과 수: {}", 
						lawId, articleKey, documents.size());
				if (!documents.isEmpty()) {
					log.info("연관 법령 조문 검색 성공 - lawId: {}, articleKey: {}, 유사도 점수: {}", 
							lawId, articleKey, documents.get(0).getScore());
				} else {
					log.warn("연관 법령 조문 검색 실패 - lawId: {}, articleKey: {}, 임계값: {}", 
							lawId, articleKey, effectiveThreshold);
					// 임계값을 0으로 낮춰서 다시 시도
					SearchRequest retryRequest = SearchRequest.builder()
							.query(query)
							.topK(topK)
							.similarityThreshold(0.0)
							.filterExpression(
									String.format("documentType == 'lawArticle' && lawId == '%s' && articleKey == '%s'",
											lawId, articleKey))
							.build();
					List<Document> retryDocuments = vectorStore.similaritySearch(retryRequest);
					if (!retryDocuments.isEmpty()) {
						log.info("연관 법령 조문 검색 재시도 성공 - lawId: {}, articleKey: {}, 유사도 점수: {}", 
								lawId, articleKey, retryDocuments.get(0).getScore());
						allResults.addAll(retryDocuments);
					} else {
						log.warn("연관 법령 조문 검색 재시도 실패 - lawId: {}, articleKey: {}, 벡터 스토어에 해당 법령 조문이 없을 수 있습니다", 
								lawId, articleKey);
					}
				}
				allResults.addAll(documents);

			} catch (Exception e) {
				log.warn("법령 조문 검색 중 오류 발생 - lawId: {}, articleKey: {}", lawId, articleKey, e);
			}
		}

		// 유사도 점수 기준으로 정렬 및 상위 결과만 반환
		return allResults.stream()
				.sorted((d1, d2) -> {
					Double score1 = d1.getScore() != null ? d1.getScore() : 0.0;
					Double score2 = d2.getScore() != null ? d2.getScore() : 0.0;
					return score2.compareTo(score1);
				})
				.limit(topK)
				.toList();
	}

	/**
	 * Document를 SearchResult로 변환 (상담 데이터용)
	 * 
	 * @param document Vector Store에서 반환된 Document
	 * @return SearchResult
	 */
	private SearchResult convertToSearchResult(Document document) {
		Map<String, Object> metadata = document.getMetadata();

		// 메타데이터에서 정보 추출
		Long counselId = extractLong(metadata, "counselId");
		String title = extractString(metadata, "title");
		String fieldLarge = extractFieldLargeName(metadata, "fieldLarge");
		String createdAt = extractString(metadata, "createdAt");
		Integer chunkIndex = extractInteger(metadata, "chunkIndex");
		Integer totalChunks = extractInteger(metadata, "totalChunks");
		String documentType = extractString(metadata, "documentType");

		// 유사도 점수 추출 (Document의 getScore() 메서드 사용)
		Double similarityScore = document.getScore();

		// 법령 조문 정보 추출 및 벡터 검색
		List<SearchResult.LawArticleInfo> lawArticles = extractAndSearchLawArticles(metadata);

		return SearchResult.builder()
				.counselId(counselId)
				.title(title)
				.content(document.getText()) // 청크 내용 (getText() 또는 getContent() 사용)
				.fieldLarge(fieldLarge)
				.createdAt(createdAt)
				.chunkIndex(chunkIndex)
				.totalChunks(totalChunks)
				.similarityScore(similarityScore)
				.documentType(documentType != null ? documentType : "counsel")
				.lawArticles(lawArticles)
				.build();
	}

	/**
	 * 법령 조문 Document를 SearchResult로 변환
	 * 
	 * @param document 법령 조문 Document
	 * @return SearchResult
	 */
	private SearchResult convertLawArticleToSearchResult(Document document) {
		Map<String, Object> metadata = document.getMetadata();

		// 법령 조문 정보 추출
		String lawId = extractString(metadata, "lawId");
		String lawNameKorean = extractString(metadata, "lawNameKorean");
		String articleKey = extractString(metadata, "articleKey");
		String articleKoreanString = extractString(metadata, "articleKoreanString");
		String articleTitle = extractString(metadata, "articleTitle");
		Integer chunkIndex = extractInteger(metadata, "chunkIndex");
		Integer totalChunks = extractInteger(metadata, "totalChunks");
		String documentType = extractString(metadata, "documentType");
		
		log.debug("convertLawArticleToSearchResult - lawId: {}, articleKey: {}, documentType: {}", 
				lawId, articleKey, documentType);

		// 유사도 점수 추출
		Double similarityScore = document.getScore();

		// 제목 생성 (법령명 + 조문)
		String title = null;
		if (lawNameKorean != null && articleKoreanString != null) {
			title = lawNameKorean + " " + articleKoreanString;
			if (articleTitle != null) {
				title += " - " + articleTitle;
			}
		} else if (articleTitle != null) {
			title = articleTitle;
		}

		// 법령 조문 정보 구성
		SearchResult.LawArticleInfo lawArticleInfo = SearchResult.LawArticleInfo.builder()
				.lawId(lawId)
				.lawNameKorean(lawNameKorean)
				.articleKey(articleKey)
				.articleKoreanString(articleKoreanString)
				.articleTitle(articleTitle)
				.articleContent(document.getText())
				.build();

		return SearchResult.builder()
				.title(title)
				.content(document.getText())
				.chunkIndex(chunkIndex)
				.totalChunks(totalChunks)
				.similarityScore(similarityScore)
				.documentType(documentType != null ? documentType : "lawArticle")
				.lawArticles(List.of(lawArticleInfo))
				.build();
	}

	/**
	 * 메타데이터에서 법령 조문 정보를 추출하고 벡터 스토어에서 법령 조문 문서를 검색
	 * 
	 * @param metadata 벡터 메타데이터
	 * @return 법령 조문 정보 리스트
	 */
	@SuppressWarnings("unchecked")
	private List<SearchResult.LawArticleInfo> extractAndSearchLawArticles(Map<String, Object> metadata) {
		List<SearchResult.LawArticleInfo> lawArticleInfos = new ArrayList<>();

		try {
			// lawArticlePairs 추출 (예: ["001649:0010021"])
			Object lawArticlePairsObj = metadata.get("lawArticlePairs");
			if (lawArticlePairsObj == null) {
				return lawArticleInfos;
			}

			List<String> lawArticlePairs;
			if (lawArticlePairsObj instanceof List) {
				lawArticlePairs = (List<String>) lawArticlePairsObj;
			} else {
				log.warn("lawArticlePairs가 List 타입이 아닙니다: {}", lawArticlePairsObj.getClass());
				return lawArticleInfos;
			}

			if (lawArticlePairs.isEmpty()) {
				return lawArticleInfos;
			}

			// 각 lawArticlePair를 파싱하여 벡터 스토어에서 법령 조문 검색
			for (String pair : lawArticlePairs) {
				if (pair == null || !pair.contains(":")) {
					log.warn("잘못된 lawArticlePair 형식: {}", pair);
					continue;
				}

				String[] parts = pair.split(":", 2);
				if (parts.length != 2) {
					log.warn("lawArticlePair 파싱 실패: {}", pair);
					continue;
				}

				String lawId = parts[0].trim();
				String articleKey = parts[1].trim();

				if (lawId.isEmpty() || articleKey.isEmpty()) {
					continue;
				}

				// 벡터 스토어에서 법령 조문 검색 (documentType == 'lawArticle' 필터링)
				// 메타데이터 필터만으로 정확히 매칭되는 문서 검색
				SearchRequest lawArticleSearchRequest = SearchRequest.builder()
						.query("법령 조문") // 더미 쿼리 (메타데이터 필터가 주 목적)
						.topK(10) // 넓은 범위로 검색 후 필터링
						.similarityThreshold(0.0) // 임계값 없이 모든 결과 포함
						.filterExpression(
								String.format("documentType == 'lawArticle' && lawId == '%s' && articleKey == '%s'",
										lawId, articleKey))
						.build();

				List<Document> lawArticleDocuments = vectorStore.similaritySearch(lawArticleSearchRequest);

				if (lawArticleDocuments.isEmpty()) {
					log.debug("법령 조문을 벡터 스토어에서 찾을 수 없습니다: lawId={}, articleKey={}", lawId, articleKey);
					continue;
				}

				// 첫 번째 문서 사용 (정확히 매칭되는 문서)
				Document lawArticleDoc = lawArticleDocuments.get(0);
				Map<String, Object> lawArticleMeta = lawArticleDoc.getMetadata();

				// 법령 조문 정보 구성
				SearchResult.LawArticleInfo lawArticleInfo = SearchResult.LawArticleInfo.builder()
						.lawId(extractString(lawArticleMeta, "lawId"))
						.lawNameKorean(extractString(lawArticleMeta, "lawNameKorean"))
						.articleKey(extractString(lawArticleMeta, "articleKey"))
						.articleKoreanString(extractString(lawArticleMeta, "articleKoreanString"))
						.articleTitle(extractString(lawArticleMeta, "articleTitle"))
						.articleContent(lawArticleDoc.getText()) // 벡터 스토어에 저장된 조문 내용
						.build();

				lawArticleInfos.add(lawArticleInfo);
			}

		} catch (Exception e) {
			log.error("법령 조문 정보 추출 중 오류 발생", e);
			// 오류 발생 시 빈 리스트 반환 (검색 결과는 계속 진행)
		}

		return lawArticleInfos;
	}

	/**
	 * 메타데이터에서 Long 값 추출
	 */
	private Long extractLong(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value == null) {
			return null;
		}
		if (value instanceof Long longValue) {
			return longValue;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value instanceof String string) {
			try {
				return Long.parseLong(string);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * 메타데이터에서 String 값 추출
	 */
	private String extractString(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	/**
	 * 메타데이터에서 fieldLarge의 name 값 추출
	 * fieldLarge는 name만 저장된 문자열 (예: "국제조세")
	 */
	private String extractFieldLargeName(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value == null) {
			return null;
		}
		// name만 저장되므로 파싱 없이 그대로 반환
		return value.toString();
	}

	/**
	 * 메타데이터에서 Integer 값 추출
	 */
	private Integer extractInteger(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value == null) {
			return null;
		}
		if (value instanceof Integer integer) {
			return integer;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value instanceof String string) {
			try {
				return Integer.parseInt(string);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * 메타데이터에서 Double 값 추출
	 */
	private Double extractDouble(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value == null) {
			return null;
		}
		if (value instanceof Double doubleValue) {
			return doubleValue;
		}
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		if (value instanceof String string) {
			try {
				return Double.parseDouble(string);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * 검색 예외
	 */
	public static class SearchException extends RuntimeException {
		public SearchException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
