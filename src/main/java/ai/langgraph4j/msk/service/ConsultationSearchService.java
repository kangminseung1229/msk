package ai.langgraph4j.msk.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.langgraph4j.msk.service.dto.SearchResult;
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
				Long consultationId = extractLong(meta, "consultationId");
				String title = extractString(meta, "title");

				log.info("검색 결과 [{}] - consultationId: {}, title: {}, score: {}, content: {}...",
						i + 1, consultationId, title, score,
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
	 * Document를 SearchResult로 변환
	 * 
	 * @param document Vector Store에서 반환된 Document
	 * @return SearchResult
	 */
	private SearchResult convertToSearchResult(Document document) {
		Map<String, Object> metadata = document.getMetadata();

		// 메타데이터에서 정보 추출
		Long consultationId = extractLong(metadata, "consultationId");
		String title = extractString(metadata, "title");
		String fieldLarge = extractFieldLargeName(metadata, "fieldLarge");
		String createdAt = extractString(metadata, "createdAt");
		Integer chunkIndex = extractInteger(metadata, "chunkIndex");
		Integer totalChunks = extractInteger(metadata, "totalChunks");

		// 유사도 점수 추출 (Document의 getScore() 메서드 사용)
		Double similarityScore = document.getScore();

		return SearchResult.builder()
				.consultationId(consultationId)
				.title(title)
				.content(document.getText()) // 청크 내용 (getText() 또는 getContent() 사용)
				.fieldLarge(fieldLarge)
				.createdAt(createdAt)
				.chunkIndex(chunkIndex)
				.totalChunks(totalChunks)
				.similarityScore(similarityScore)
				.build();
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
	 * 메타데이터에서 fieldLarge의 name 값만 추출
	 * fieldLarge는 "CounselFieldLarge(id=11, name=국제조세, ...)" 형식의 문자열
	 */
	private String extractFieldLargeName(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value == null) {
			return null;
		}

		String fieldLargeStr = value.toString();
		// "name=" 다음의 값을 추출 (쉼표나 닫는 괄호 전까지)
		int nameIndex = fieldLargeStr.indexOf("name=");
		if (nameIndex == -1) {
			return null;
		}

		int startIndex = nameIndex + 5; // "name=" 길이
		int endIndex = fieldLargeStr.indexOf(",", startIndex);
		if (endIndex == -1) {
			endIndex = fieldLargeStr.indexOf(")", startIndex);
		}
		if (endIndex == -1) {
			// 끝까지가 name 값인 경우
			return fieldLargeStr.substring(startIndex).trim();
		}

		return fieldLargeStr.substring(startIndex, endIndex).trim();
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
