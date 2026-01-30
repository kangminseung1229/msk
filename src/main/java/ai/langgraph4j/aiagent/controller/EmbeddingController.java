package ai.langgraph4j.aiagent.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.langgraph4j.aiagent.controller.dto.TokenEstimateResponse;
import ai.langgraph4j.aiagent.controller.dto.YpTokenEstimateResponse;
import ai.langgraph4j.aiagent.entity.law.TaxLawCode;
import ai.langgraph4j.aiagent.repository.TaxLawCodeRepository;
import ai.langgraph4j.aiagent.service.CounselEmbeddingService;
import ai.langgraph4j.aiagent.service.LawArticleEmbeddingService;
import ai.langgraph4j.aiagent.service.YpEmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 임베딩 관리 컨트롤러
 * 상담 데이터와 법령 조문 데이터를 임베딩하여 Vector Store에 저장하는 API를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/embedding")
@RequiredArgsConstructor
@Tag(name = "Embedding", description = "임베딩 관리 API")
public class EmbeddingController {

	private final CounselEmbeddingService counselEmbeddingService;
	private final LawArticleEmbeddingService lawArticleEmbeddingService;
	private final YpEmbeddingService ypEmbeddingService;
	private final TaxLawCodeRepository taxLawCodeRepository;

	/**
	 * 모든 상담 데이터 임베딩
	 * 
	 * @return 처리된 상담 수
	 */
	@Operation(summary = "전체 상담 데이터 임베딩", description = "모든 상담 데이터를 임베딩하여 Vector Store에 저장합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "임베딩 완료", content = @Content(schema = @Schema(implementation = EmbeddingResponse.class)))
	})
	@PostMapping("/counsel/all")
	public ResponseEntity<EmbeddingResponse> embedAllCounsel() {
		log.info("전체 상담 데이터 임베딩 요청");

		int count = counselEmbeddingService.embedAllConsultations();

		return ResponseEntity.ok(new EmbeddingResponse(
				"전체 상담 데이터 임베딩 완료",
				count));
	}

	/**
	 * 특정 상담 ID 임베딩
	 * 
	 * @param counselId 상담 ID
	 * @return 응답
	 */
	@Operation(summary = "특정 상담 데이터 임베딩", description = "지정된 상담 ID의 데이터를 임베딩하여 Vector Store에 저장합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "임베딩 완료", content = @Content(schema = @Schema(implementation = EmbeddingResponse.class))),
			@ApiResponse(responseCode = "400", description = "상담을 찾을 수 없음")
	})
	@PostMapping("/counsel/{counselId}")
	public ResponseEntity<EmbeddingResponse> embedOneCounsel(
			@Parameter(description = "상담 ID", required = true, example = "1") @PathVariable Long counselId) {
		log.info("상담 ID {} 임베딩 요청", counselId);

		counselEmbeddingService.embedConsultation(counselId);

		return ResponseEntity.ok(new EmbeddingResponse(
				"상담 ID " + counselId + " 임베딩 완료",
				1));
	}

	/**
	 * 모든 법령 조문 임베딩
	 * 
	 * @return 처리된 조문 수
	 */
	@Operation(summary = "전체 법령 조문 임베딩", description = "모든 lawId별 최신 법령의 조문들을 임베딩하여 Vector Store에 저장합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "임베딩 완료", content = @Content(schema = @Schema(implementation = EmbeddingResponse.class)))
	})
	@PostMapping("/law/all")
	public ResponseEntity<EmbeddingResponse> embedAllLawArticles() {
		log.info("전체 법령 조문 임베딩 요청");

		int count = lawArticleEmbeddingService.embedAllLatestLawArticles();

		return ResponseEntity.ok(new EmbeddingResponse(
				"전체 법령 조문 임베딩 완료",
				count));
	}

	@PostMapping("/law/tax")
	public ResponseEntity<EmbeddingResponse> embedTaxLawArticles() {

		List<TaxLawCode> taxLawCodes = taxLawCodeRepository.findAll();
		for (TaxLawCode taxLawCode : taxLawCodes) {
			lawArticleEmbeddingService.embedLatestLawArticlesByLawId(taxLawCode.getLawId());
		}
		return ResponseEntity.ok(new EmbeddingResponse(
				"세무법 표현 임베딩 완료",
				taxLawCodes.size()));
	}

	/**
	 * 특정 법령 ID의 조문 임베딩
	 * 
	 * @param lawId 법령 ID
	 * @return 응답
	 */
	@Operation(summary = "특정 법령 조문 임베딩", description = "지정된 법령 ID의 최신 조문들을 임베딩하여 Vector Store에 저장합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "임베딩 완료", content = @Content(schema = @Schema(implementation = EmbeddingResponse.class))),
			@ApiResponse(responseCode = "400", description = "법령을 찾을 수 없음")
	})
	@PostMapping("/law/{lawId}")
	public ResponseEntity<EmbeddingResponse> embedLawArticlesByLawId(
			@Parameter(description = "법령 ID", required = true, example = "법령ID001") @PathVariable String lawId) {
		log.info("법령 ID {} 임베딩 요청", lawId);

		int count = lawArticleEmbeddingService.embedLatestLawArticlesByLawId(lawId);

		return ResponseEntity.ok(new EmbeddingResponse(
				"법령 ID " + lawId + " 임베딩 완료",
				count));
	}

	/**
	 * 특정 법령 ID의 조문 임베딩 토큰 수 및 비용 예상 계산
	 * 
	 * @param lawId 법령 ID
	 * @return 토큰 계산 결과
	 */
	@Operation(summary = "법령 조문 임베딩 토큰 수 및 비용 예상 계산", description = "지정된 법령 ID의 최신 조문들을 임베딩할 때 예상되는 토큰 수와 비용을 계산합니다. "
			+ "lawKey 기준 최신 LawBasicInformation을 구하고, LawArticleMetadata로 임베딩한다고 가정하여 계산합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "토큰 계산 완료", content = @Content(schema = @Schema(implementation = TokenEstimateResponse.class))),
			@ApiResponse(responseCode = "400", description = "법령을 찾을 수 없음")
	})
	@GetMapping("/law/{lawId}/token-estimate")
	public ResponseEntity<TokenEstimateResponse> estimateTokensByLawId(
			@Parameter(description = "법령 ID", required = true, example = "법령ID001") @PathVariable String lawId) {
		log.info("법령 ID {} 토큰 계산 요청", lawId);

		TokenEstimateResponse response = lawArticleEmbeddingService.estimateTokensByLawId(lawId);

		return ResponseEntity.ok(response);
	}

	/**
	 * 예규판례 상위 100건 임베딩 토큰 수·비용 예상 계산
	 *
	 * @return 토큰 계산 결과
	 */
	@Operation(summary = "예규판례 상위 100건 토큰 계산", description = "삭제되지 않은 예규판례 상위 100건을 임베딩할 때 예상 토큰 수와 비용을 계산합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "토큰 계산 완료", content = @Content(schema = @Schema(implementation = YpTokenEstimateResponse.class)))
	})
	@GetMapping("/yp/token-estimate-top100")
	public ResponseEntity<YpTokenEstimateResponse> estimateYpTokensTop100() {
		log.info("예규판례 상위 100건 토큰 계산 요청");
		YpTokenEstimateResponse response = ypEmbeddingService.estimateTokensTop100();
		return ResponseEntity.ok(response);
	}

	/**
	 * 예규판례 임베딩 (최근 N년 이내 document_date 기준, 페이징으로 전체 대상)
	 *
	 * @param recentYears 최근 몇 년 이내 (기본 5). document_date(yyyyMMdd)가 이 기간 이내인 건을 페이징으로 전부 임베딩.
	 * @return 처리된 문서 수 (청크 포함)
	 */
	@Operation(summary = "예규판례 임베딩 (최근 N년)", description = "document_date가 최근 N년 이내인 예규판례를 페이징으로 전부 임베딩하여 Vector Store에 저장합니다. document_date 형식: yyyyMMdd")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "임베딩 완료", content = @Content(schema = @Schema(implementation = EmbeddingResponse.class)))
	})
	@PostMapping("/yp/top100")
	public ResponseEntity<EmbeddingResponse> embedYpByRecentYears(
			@Parameter(description = "최근 몇 년 이내 (document_date 기준)", example = "5") @RequestParam(defaultValue = "5") int recentYears) {
		log.info("예규판례 임베딩 요청: 최근 {}년", recentYears);
		int years = recentYears > 0 ? recentYears : 5;
		int count = ypEmbeddingService.embedByRecentYears(years);
		return ResponseEntity.ok(new EmbeddingResponse("예규판례 최근 " + years + "년 이내 임베딩 완료", count));
	}

	/**
	 * 임베딩 응답 DTO
	 */
	@Schema(description = "임베딩 처리 결과")
	public record EmbeddingResponse(
			@Schema(description = "처리 결과 메시지", example = "전체 상담 데이터 임베딩 완료") String message,
			@Schema(description = "처리된 데이터 수", example = "100") int processedCount) {
	}
}
