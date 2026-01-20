package ai.langgraph4j.msk.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.langgraph4j.msk.controller.dto.SystemInstructionRequest;
import ai.langgraph4j.msk.controller.dto.TextGenerationRequest;
import ai.langgraph4j.msk.controller.dto.TextGenerationResponse;
import ai.langgraph4j.msk.service.GeminiTextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google Gemini API를 사용한 텍스트 생성 컨트롤러
 * 샘플 코드를 기반으로 작성된 REST API 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiTextController {

	private final GeminiTextService geminiTextService;

	/**
	 * POST 요청으로 텍스트 생성
	 * 
	 * @param request 텍스트 생성 요청 (JSON)
	 * @return 생성된 텍스트 응답
	 */
	@PostMapping("/generate")
	public ResponseEntity<TextGenerationResponse> generateText(@RequestBody TextGenerationRequest request) {
		log.info("GeminiTextController: 텍스트 생성 요청 - {}", request.getPrompt());

		try {
			String model = (request.getModel() != null && !request.getModel().isEmpty())
					? request.getModel()
					: "gemini-3-flash-preview";

			String response = geminiTextService.generateText(request.getPrompt(), model);

			TextGenerationResponse responseDto = new TextGenerationResponse();
			responseDto.setResponse(response);
			responseDto.setModel(model);

			return ResponseEntity.ok(responseDto);
		} catch (Exception e) {
			log.error("GeminiTextController: 텍스트 생성 중 오류 발생", e);
			throw new RuntimeException("텍스트 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	/**
	 * GET 요청으로 간단한 텍스트 생성 (쿼리 파라미터 사용)
	 * 
	 * @param prompt 사용자 입력 프롬프트
	 * @param model  사용할 모델명 (선택사항, 기본값: gemini-3-flash-preview)
	 * @return 생성된 텍스트 응답
	 */
	@GetMapping("/generate")
	public ResponseEntity<TextGenerationResponse> generateTextGet(
			@RequestParam(name = "prompt") String prompt,
			@RequestParam(name = "model", required = false, defaultValue = "gemini-3-flash-preview") String model) {
		log.info("GeminiTextController: GET 텍스트 생성 요청 - 모델: {}, 프롬프트: {}", model, prompt);

		try {
			String response = geminiTextService.generateText(prompt, model);

			TextGenerationResponse responseDto = new TextGenerationResponse();
			responseDto.setResponse(response);
			responseDto.setModel(model);

			return ResponseEntity.ok(responseDto);
		} catch (Exception e) {
			log.error("GeminiTextController: 텍스트 생성 중 오류 발생", e);
			throw new RuntimeException("텍스트 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	@PostMapping("/thinking")
	public ResponseEntity<String> thinking(@RequestBody String prompt) {
		return ResponseEntity.ok(geminiTextService.thinking(prompt));
	}

	/**
	 * System Instruction을 사용하여 텍스트를 생성합니다.
	 * System Instruction은 모델의 역할, 동작 방식, 응답 스타일 등을 정의합니다.
	 * 
	 * @param request System Instruction과 User Prompt를 포함한 요청
	 * @return 생성된 텍스트 응답
	 */
	@PostMapping("/system-instruction")
	public ResponseEntity<String> systemInstruction(@RequestBody SystemInstructionRequest request) {
		String model = (request.getModel() != null && !request.getModel().isEmpty())
				? request.getModel()
				: "gemini-3-flash-preview";

		return ResponseEntity.ok(geminiTextService.systemInstruction(
				request.getSystemInstruction(),
				request.getUserPrompt(),
				model));
	}

	@PostMapping("/streaming")
	public ResponseEntity<String> streaming(@RequestBody SystemInstructionRequest request) {
		geminiTextService.streaming(request.getSystemInstruction(), request.getUserPrompt(), request.getModel());
		return ResponseEntity.ok("streaming completed");
	}

	/**
	 * 타임리프 스트리밍 페이지를 반환합니다.
	 * 
	 * @return 스트리밍 UI 페이지
	 */
	@GetMapping("/streaming-page")
	public ModelAndView streamingPage() {
		return new ModelAndView("streaming");
	}

	/**
	 * SSE를 사용한 스트리밍 응답 엔드포인트
	 * 
	 * @param systemInstruction System Instruction
	 * @param userPrompt        User Prompt
	 * @param model             모델명
	 * @return SseEmitter
	 */
	@GetMapping(value = "/streaming-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamingSse(
			@RequestParam(name = "systemInstruction", required = false) String systemInstruction,
			@RequestParam(name = "userPrompt") String userPrompt,
			@RequestParam(name = "model", required = false) String model) {
		log.info("스트리밍 요청 - systemInstruction: {}, userPrompt: {}, model: {}",
				systemInstruction, userPrompt, model);
		return geminiTextService.streamingSse(systemInstruction, userPrompt, model);
	}

}
