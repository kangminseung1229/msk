package ai.langgraph4j.msk.controller;

import org.springframework.http.HttpStatus;
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

import ai.langgraph4j.msk.controller.dto.AgentRequest;
import ai.langgraph4j.msk.controller.dto.AgentResponse;
import ai.langgraph4j.msk.controller.dto.ErrorResponse;
import ai.langgraph4j.msk.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 에이전트 테스트 컨트롤러
 * Phase 2 테스트를 위한 간단한 REST API 엔드포인트
 * 
 * Phase 3: 서비스 레이어 분리로 컨트롤러는 요청/응답 처리만 담당
 */
@Slf4j
@RestController
@RequestMapping("/api/test/agent")
@RequiredArgsConstructor
public class AgentTestController {

	private final AgentService agentService;

	/**
	 * 에이전트 실행 테스트
	 * 
	 * @param request 에이전트 실행 요청
	 * @return 에이전트 응답
	 */
	@PostMapping("/invoke")
	public ResponseEntity<?> invoke(@Valid @RequestBody AgentRequest request) {
		log.info("AgentTestController: 에이전트 실행 요청 - {}", request.getMessage());

		try {
			AgentResponse response = agentService.invoke(request);
			return ResponseEntity.ok(response);
		} catch (AgentService.AgentExecutionException e) {
			log.error("AgentTestController: 에이전트 실행 오류 - {}", e.getMessage());
			ErrorResponse errorResponse = ErrorResponse.builder()
					.errorCode(e.getErrorCode())
					.message(e.getMessage())
					.build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		} catch (Exception e) {
			log.error("AgentTestController: 에이전트 실행 중 오류 발생", e);
			ErrorResponse errorResponse = ErrorResponse.builder()
					.errorCode("INTERNAL_ERROR")
					.message("에이전트 실행 중 오류가 발생했습니다: " + e.getMessage())
					.build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	/**
	 * 간단한 헬스 체크
	 */
	@GetMapping("/health")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("Agent Test Controller is running");
	}

	/**
	 * 스트리밍 테스트 페이지
	 * 
	 * @return 스트리밍 UI 페이지
	 */
	@GetMapping("/streaming-page")
	public ModelAndView streamingPage() {
		return new ModelAndView("streaming");
	}

	/**
	 * 간단한 테스트 (GET 요청)
	 */
	@GetMapping("/test")
	public ResponseEntity<AgentResponse> test(@RequestParam(defaultValue = "안녕하세요") String message) {
		log.info("AgentTestController: 간단한 테스트 요청 - {}", message);

		AgentRequest request = AgentRequest.builder()
				.message(message)
				.build();

		ResponseEntity<?> response = invoke(request);

		if (response.getBody() instanceof AgentResponse) {
			return ResponseEntity.ok((AgentResponse) response.getBody());
		} else {
			// 에러 응답인 경우
			return ResponseEntity.status(response.getStatusCode())
					.body(AgentResponse.builder()
							.response("테스트 실패")
							.build());
		}
	}

	/**
	 * Phase 3: 스트리밍 응답 지원 (POST)
	 * SSE(Server-Sent Events)를 사용하여 에이전트 실행 중간 결과를 실시간으로 전송합니다.
	 * 
	 * @param request 에이전트 실행 요청
	 * @return SseEmitter 스트리밍 응답을 위한 emitter
	 */
	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@Valid @RequestBody AgentRequest request) {
		log.info("AgentTestController: 스트리밍 요청 (POST) - {}", request.getMessage());
		return agentService.stream(request.getMessage(), request.getSessionId(), request.getSystemInstruction());
	}

	/**
	 * Phase 3: 스트리밍 응답 지원 (GET)
	 * SSE(Server-Sent Events)를 사용하여 에이전트 실행 중간 결과를 실시간으로 전송합니다.
	 * 
	 * @param message           사용자 메시지
	 * @param sessionId         세션 ID (선택사항)
	 * @param systemInstruction System Instruction (선택사항)
	 * @return SseEmitter 스트리밍 응답을 위한 emitter
	 */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamGet(
			@RequestParam(name = "message") String message,
			@RequestParam(name = "sessionId", required = false) String sessionId,
			@RequestParam(name = "systemInstruction", required = false) String systemInstruction) {
		log.info("AgentTestController: 스트리밍 요청 (GET) - {}", message);
		return agentService.stream(message, sessionId, systemInstruction);
	}
}
