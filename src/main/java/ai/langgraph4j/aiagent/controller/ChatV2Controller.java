package ai.langgraph4j.aiagent.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.langgraph4j.aiagent.controller.dto.ChatV2Request;
import ai.langgraph4j.aiagent.controller.dto.ChatV2Response;
import ai.langgraph4j.aiagent.controller.dto.ErrorResponse;
import ai.langgraph4j.aiagent.service.ChatSessionPersistenceService;
import ai.langgraph4j.aiagent.service.ChatV2Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 v2 컨트롤러
 * LangGraph 기반 채팅 시스템의 REST API
 * 
 * 주요 기능:
 * - 세션 기반 대화 컨텍스트 유지 (Redis)
 * - LangGraph를 활용한 에이전트 실행
 * - 답변 검수 기능
 * - 스트리밍 지원
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/chat")
@RequiredArgsConstructor
public class ChatV2Controller {

	private final ChatV2Service chatV2Service;
	private final ChatSessionPersistenceService chatSessionPersistenceService;

	/**
	 * 채팅 실행 (비스트리밍)
	 * 
	 * @param request 채팅 요청
	 * @return 채팅 응답
	 */
	@PostMapping
	public ResponseEntity<?> chat(@Valid @RequestBody ChatV2Request request) {
		log.info("ChatV2Controller: 채팅 요청 - message: {}, sessionId: {}",
				request.getMessage(), request.getSessionId());

		try {
			ChatV2Response response = chatV2Service.chat(request);
			return ResponseEntity.ok(response);
		} catch (ChatV2Service.ChatV2Exception e) {
			log.error("ChatV2Controller: 채팅 실행 오류 - {}", e.getMessage());
			ErrorResponse errorResponse = ErrorResponse.builder()
					.errorCode(e.getErrorCode())
					.message(e.getMessage())
					.build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		} catch (Exception e) {
			log.error("ChatV2Controller: 채팅 실행 중 오류 발생", e);
			ErrorResponse errorResponse = ErrorResponse.builder()
					.errorCode("INTERNAL_ERROR")
					.message("채팅 실행 중 오류가 발생했습니다: " + e.getMessage())
					.build();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	/**
	 * 채팅 실행 (스트리밍)
	 * 
	 * @param request 채팅 요청
	 * @return SseEmitter
	 */
	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter chatStreaming(@Valid @RequestBody ChatV2Request request) {
		log.info("ChatV2Controller: 스트리밍 채팅 요청 - message: {}, sessionId: {}",
				request.getMessage(), request.getSessionId());
		return chatV2Service.chatStreaming(request);
	}

	/**
	 * 채팅 실행 (GET 요청, 비스트리밍)
	 * 
	 * @param message           사용자 메시지
	 * @param sessionId         세션 ID (선택사항)
	 * @param systemInstruction System Instruction (선택사항)
	 * @return 채팅 응답
	 */
	@GetMapping
	public ResponseEntity<?> chatGet(
			@RequestParam(name = "message") String message,
			@RequestParam(name = "sessionId", required = false) String sessionId,
			@RequestParam(name = "systemInstruction", required = false) String systemInstruction) {

		ChatV2Request request = ChatV2Request.builder()
				.message(message)
				.sessionId(sessionId)
				.systemInstruction(systemInstruction)
				.build();

		return chat(request);
	}

	/**
	 * 채팅 실행 (GET 요청, 스트리밍)
	 * 
	 * @param message           사용자 메시지
	 * @param sessionId         세션 ID (선택사항)
	 * @param systemInstruction System Instruction (선택사항)
	 * @return SseEmitter
	 */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter chatStreamingGet(
			@RequestParam(name = "message") String message,
			@RequestParam(name = "sessionId", required = false) String sessionId,
			@RequestParam(name = "systemInstruction", required = false) String systemInstruction) {

		ChatV2Request request = ChatV2Request.builder()
				.message(message)
				.sessionId(sessionId)
				.systemInstruction(systemInstruction)
				.build();

		return chatStreaming(request);
	}

	/**
	 * 세션 목록 조회 (왼쪽 탭용, 최신순)
	 *
	 * @param page 페이지 (0부터)
	 * @param size 페이지 크기
	 * @return 세션 목록
	 */
	@GetMapping("/sessions")
	public ResponseEntity<?> listSessions(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "50") int size) {
		return ResponseEntity.ok(chatSessionPersistenceService.listSessions(page, size));
	}

	/**
	 * 세션의 대화 히스토리 조회
	 *
	 * @param sessionId 세션 ID (예: session-uuid)
	 * @return 세션 정보 + 메시지 목록
	 */
	@GetMapping("/sessions/{sessionId}")
	public ResponseEntity<?> getSessionWithMessages(@PathVariable String sessionId) {
		return chatSessionPersistenceService.getSessionWithMessages(sessionId)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * DB에 저장된 세션을 Redis에 복원 (이전 세션 클릭 시 문맥 유지)
	 *
	 * @param sessionId 세션 ID
	 */
	@PostMapping("/sessions/{sessionId}/restore")
	public ResponseEntity<Void> restoreSession(@PathVariable String sessionId) {
		chatV2Service.restoreSessionFromDbToRedis(sessionId);
		return ResponseEntity.ok().build();
	}

	/**
	 * 채팅 v2 데모 페이지
	 * 
	 * @return 데모 페이지
	 */
	@GetMapping("/demo")
	public ModelAndView demoPage() {
		return new ModelAndView("chat-v2-demo");
	}
}
