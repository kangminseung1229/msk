package ai.langgraph4j.aiagent.metadata;

import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import ai.langgraph4j.aiagent.entity.yp.Yp;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 예규·판례(Yp) 문서의 벡터 임베딩 메타데이터.
 * 현재 구성된 메타데이터와 연관되어 관련 예규판례 검색에 사용됩니다.
 */
@Slf4j
@Data
@Builder
public class YpMetadata implements DocumentMetadata {

	public static final String DOCUMENT_TYPE = "yp";

	// 예규판례 기본 정보
	private Long ypId;
	private String taxLawCodeLawId;

	// 등록 유형 (세무, 세무/일반, 일반, 노무, 행정해석)
	private String registType;

	// 문선 번호
	private String documentNumber;

	// 문서 일자
	private String documentDate;

	// 제목
	private String title;

	// 예판 구분 (사전답변, 질의회신, 과세자문, 심사청구, 심판청구, 판례, 헌재)
	private String ypGubun;

	// 예판 유형 (인용, 일부인용, 경정, 취소, 재조사, 국패, 국승 등)
	private String ypType;

	// 메인 여부
	private Boolean mainYn;

	// 전문가 추천 여부
	private Boolean recommendYn;

	// 청크 정보 (본문 분할 시)
	private Integer chunkIndex;
	private Integer totalChunks;

	/**
	 * Yp 엔티티로부터 YpMetadata 생성
	 *
	 * @param yp 예규판례 엔티티
	 * @return YpMetadata
	 */
	public static YpMetadata from(Yp yp) {
		YpMetadataBuilder builder = YpMetadata.builder()
				.ypId(yp.getId())
				.documentNumber(yp.getDocumentNumber())
				.documentDate(yp.getDocumentDate())
				.title(yp.getTitle())
				.mainYn(yp.isMainYn())
				.recommendYn(yp.isRecommendYn());

		if (yp.getTaxLawCodeLawId() != null) {
			builder.taxLawCodeLawId(yp.getTaxLawCodeLawId());
		}
		if (yp.getRegistType() != null) {
			builder.registType(yp.getRegistType().name());
		}
		if (yp.getYpGubun() != null) {
			builder.ypGubun(yp.getYpGubun().name());
		}
		if (yp.getYpType() != null) {
			builder.ypType(yp.getYpType().name());
		}

		return builder.build();
	}

	/**
	 * Map으로부터 YpMetadata 생성 (벡터 검색 결과에서 사용)
	 *
	 * @param metadataMap 메타데이터 Map
	 * @return YpMetadata
	 */
	public static YpMetadata fromMap(Map<String, Object> metadataMap) {
		YpMetadataBuilder builder = YpMetadata.builder()
				.ypId(extractLong(metadataMap, "ypId"))
				.taxLawCodeLawId(extractString(metadataMap, "taxLawCodeLawId"))
				.registType(extractString(metadataMap, "registType"))
				.documentNumber(extractString(metadataMap, "documentNumber"))
				.documentDate(extractString(metadataMap, "documentDate"))
				.title(extractString(metadataMap, "title"))
				.ypGubun(extractString(metadataMap, "ypGubun"))
				.ypType(extractString(metadataMap, "ypType"))
				.chunkIndex(extractInteger(metadataMap, "chunkIndex"))
				.totalChunks(extractInteger(metadataMap, "totalChunks"));

		if (metadataMap.get("mainYn") != null) {
			builder.mainYn(extractBoolean(metadataMap, "mainYn"));
		}
		if (metadataMap.get("recommendYn") != null) {
			builder.recommendYn(extractBoolean(metadataMap, "recommendYn"));
		}

		return builder.build();
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();

		map.put("documentType", DOCUMENT_TYPE);

		if (ypId != null) {
			map.put("ypId", ypId);
		}
		if (taxLawCodeLawId != null) {
			map.put("taxLawCodeLawId", taxLawCodeLawId);
		}
		if (registType != null) {
			map.put("registType", registType);
		}
		if (documentNumber != null) {
			map.put("documentNumber", documentNumber);
		}
		if (documentDate != null) {
			map.put("documentDate", documentDate);
		}
		if (title != null) {
			map.put("title", title);
		}
		if (ypGubun != null) {
			map.put("ypGubun", ypGubun);
		}
		if (ypType != null) {
			map.put("ypType", ypType);
		}
		if (mainYn != null) {
			map.put("mainYn", mainYn);
		}
		if (recommendYn != null) {
			map.put("recommendYn", recommendYn);
		}
		if (chunkIndex != null) {
			map.put("chunkIndex", chunkIndex);
		}
		if (totalChunks != null) {
			map.put("totalChunks", totalChunks);
		}

		return map;
	}

	@Override
	public String getDocumentType() {
		return DOCUMENT_TYPE;
	}

	/**
	 * 예규판례 데이터로부터 임베딩할 텍스트 생성.
	 * 제목·내용을 합치고, HTML 태그는 제거합니다.
	 *
	 * @param yp 예규판례 엔티티
	 * @return 임베딩할 텍스트 (HTML 제거됨)
	 */
	public static String buildYpText(Yp yp) {
		StringBuilder text = new StringBuilder();

		if (yp.getTitle() != null && !yp.getTitle().trim().isEmpty()) {
			text.append("제목: ").append(removeHtmlTags(yp.getTitle())).append("\n");
		}
		if (yp.getDocumentNumber() != null && !yp.getDocumentNumber().trim().isEmpty()) {
			text.append("문서번호: ").append(yp.getDocumentNumber()).append("\n");
		}
		if (yp.getDocumentDate() != null && !yp.getDocumentDate().trim().isEmpty()) {
			text.append("문서일자: ").append(yp.getDocumentDate()).append("\n");
		}
		if (yp.getContent() != null && !yp.getContent().trim().isEmpty()) {
			text.append("내용: ").append(removeHtmlTags(yp.getContent())).append("\n");
		}

		return text.toString().trim();
	}

	private static String removeHtmlTags(String html) {
		if (html == null || html.trim().isEmpty()) {
			return "";
		}
		try {
			String cleanText = Jsoup.clean(html, Safelist.none());
			cleanText = Jsoup.parse(cleanText).text();
			return cleanText.replaceAll("\\s+", " ").trim();
		} catch (Exception e) {
			log.warn("HTML 태그 제거 중 오류 발생, 원본 텍스트 반환: {}", e.getMessage());
			return html.replaceAll("<[^>]+>", "").trim();
		}
	}

	// --- 유틸리티 (검색 결과 파싱) ---
	private static Long extractLong(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value == null) return null;
		if (value instanceof Long) return (Long) value;
		if (value instanceof Number) return ((Number) value).longValue();
		if (value instanceof String) {
			try { return Long.parseLong((String) value); } catch (NumberFormatException e) { return null; }
		}
		return null;
	}

	private static String extractString(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return value != null ? value.toString() : null;
	}

	private static Integer extractInteger(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value == null) return null;
		if (value instanceof Integer) return (Integer) value;
		if (value instanceof Number) return ((Number) value).intValue();
		if (value instanceof String) {
			try { return Integer.parseInt((String) value); } catch (NumberFormatException e) { return null; }
		}
		return null;
	}

	private static Boolean extractBoolean(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value == null) return null;
		if (value instanceof Boolean) return (Boolean) value;
		if (value instanceof String) return Boolean.parseBoolean((String) value);
		return null;
	}
}
