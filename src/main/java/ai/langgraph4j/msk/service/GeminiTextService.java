package ai.langgraph4j.msk.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.ThinkingLevel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Google Gemini API를 사용한 텍스트 생성 서비스
 * 샘플 코드를 기반으로 작성된 서비스 클래스
 * 
 * Phase 3: Spring AI Tool 자동 호출 지원
 * Spring AI ChatModel을 사용하여 Tool 자동 호출 기능을 활용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiTextService {

	private final Client client;
	private final ChatModel chatModel;

	/**
	 * 텍스트 입력을 받아 Gemini API로 응답을 생성합니다.
	 * 
	 * @param prompt 사용자 입력 프롬프트
	 * @param model  사용할 모델명 (기본값: gemini-3-flash-preview)
	 * @return 생성된 텍스트 응답
	 */
	public String generateText(String prompt, String model) {
		return generateText(prompt, model, false);
	}

	/**
	 * 텍스트 입력을 받아 Gemini API로 응답을 생성합니다.
	 * 
	 * @param prompt          사용자 입력 프롬프트
	 * @param model           사용할 모델명 (기본값: gemini-3-flash-preview)
	 * @param includeThoughts thoughts 포함 여부
	 * @return 생성된 텍스트 응답
	 */
	public String generateText(String prompt, String model, Boolean includeThoughts) {
		log.info("GeminiTextService: 텍스트 생성 요청 - 모델: {}, 프롬프트: {}, includeThoughts: {}",
				model, prompt, includeThoughts);

		try {
			String modelName = (model != null && !model.isEmpty()) ? model : "gemini-3-flash-preview";

			GenerateContentConfig config = null;
			if (includeThoughts != null && includeThoughts) {
				// Google GenAI Java SDK의 ThinkingConfig.Builder 사용
				// Spring AI 1.1.1과 Google GenAI SDK 1.35.0에서 includeThoughts 지원
				ThinkingConfig.Builder thinkingConfigBuilder = ThinkingConfig.builder();
				
				// 모델에 따라 thinkingLevel 또는 thinkingBudget 설정
				boolean isGemini3Model = modelName != null && modelName.contains("gemini-3");
				
				if (isGemini3Model) {
					// Gemini 3 모델: thinkingLevel 사용
					thinkingConfigBuilder.thinkingLevel(new ThinkingLevel("high"));
				} else {
					// Gemini 2.5 이전 모델: thinkingBudget 사용
					thinkingConfigBuilder.thinkingBudget(4096);
				}
				
				// includeThoughts 메서드 호출 시도 (Google GenAI SDK 1.35.0에서 지원)
				try {
					// 먼저 직접 메서드 호출 시도
					java.lang.reflect.Method includeThoughtsMethod = thinkingConfigBuilder.getClass()
							.getMethod("includeThoughts", boolean.class);
					includeThoughtsMethod.invoke(thinkingConfigBuilder, true);
					log.info("includeThoughts(true) 설정 완료 (Gemini 3 모델: {})", isGemini3Model);
				} catch (NoSuchMethodException e) {
					// includeThoughts 메서드가 없는 경우 (구버전 SDK)
					log.warn("includeThoughts 메서드를 찾을 수 없습니다. " +
							"Google GenAI SDK를 1.35.0 이상으로 업그레이드하거나 " +
							"Spring AI의 GoogleGenAiChatOptions를 사용하세요.");
				} catch (Exception e) {
					log.warn("includeThoughts 메서드 호출 중 오류: {}", e.getMessage());
				}

				ThinkingConfig thinkingConfig = thinkingConfigBuilder.build();
				config = GenerateContentConfig.builder()
						.thinkingConfig(thinkingConfig)
						.build();
			}

			GenerateContentResponse response = client.models.generateContent(
					modelName,
					prompt,
					config);

			// 응답에서 grounding_metadata와 thought 필드 확인
			if (includeThoughts != null && includeThoughts) {
				log.info("=== 응답 메타데이터 확인 ===");
				log.info("Response: {}", response);

				// candidates 확인
				List<Candidate> candidates = response.candidates().orElse(Collections.emptyList());
				if (!candidates.isEmpty()) {
					Candidate candidate = candidates.get(0);
					log.info("Candidate: {}", candidate);

					// grounding_metadata 확인
					candidate.groundingMetadata().ifPresentOrElse(
							metadata -> log.info("grounding_metadata 발견: {}", metadata),
							() -> log.info("grounding_metadata: 없음"));

					// content의 parts에서 thought 확인
					candidate.content().ifPresent(content -> {
						log.info("Content parts 확인:");
						List<Part> parts = content.parts().orElse(Collections.emptyList());
						for (Part part : parts) {
							// thought 확인
							part.thought().ifPresentOrElse(
									thought -> {
										if (thought) {
											log.info("Thought part 발견: {}", part.text().orElse(""));
										}
									},
									() -> {
										// thought가 false이거나 없으면 일반 텍스트
										part.text().ifPresent(text -> log.info("일반 텍스트 part: {}", text));
									});
						}
					});

					// Candidate의 모든 메서드 확인 (디버깅용)
					log.info("Candidate의 주요 메서드 확인:");
					log.info("  finishReason: {}", candidate.finishReason().orElse(null));
					log.info("  safetyRatings: {}", candidate.safetyRatings().orElse(Collections.emptyList()));
				} else {
					log.warn("응답에 candidates가 없습니다.");
				}
				log.info("=== 응답 메타데이터 확인 완료 ===");
			}

			String result = response.text();
			log.info("GeminiTextService: 텍스트 생성 완료 - 응답 길이: {}자", result.length());

			return result;
		} catch (RuntimeException e) {
			log.error("GeminiTextService: 텍스트 생성 중 오류 발생", e);
			throw e;
		} catch (Exception e) {
			log.error("GeminiTextService: 텍스트 생성 중 오류 발생", e);
			throw new RuntimeException("텍스트 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	/**
	 * 기본 모델(gemini-3-flash-preview)을 사용하여 텍스트를 생성합니다.
	 * 
	 * @param prompt 사용자 입력 프롬프트
	 * @return 생성된 텍스트 응답
	 */
	public String generateText(String prompt) {
		return generateText(prompt, "gemini-3-flash-preview", false);
	}

	public String thinking(String param) {
		GenerateContentConfig config = GenerateContentConfig.builder()
				.thinkingConfig(ThinkingConfig.builder()
						.thinkingLevel(new ThinkingLevel("low")))
				.build();

		GenerateContentResponse response = client.models.generateContent("gemini-3-flash-preview",
				param != null ? param : "How does AI work?", config);

		return response.text();
	}

	/**
	 * System Instruction을 사용하여 텍스트를 생성합니다.
	 * System Instruction은 모델의 역할, 동작 방식, 응답 스타일 등을 정의합니다.
	 * 
	 * @param systemInstruction 모델의 역할과 동작 방식을 정의하는 지시사항
	 * @param userPrompt        실제 사용자의 질문이나 요청
	 * @param model             사용할 모델명 (선택사항, 기본값: gemini-3-flash-preview)
	 * @return 생성된 텍스트 응답
	 */
	public String systemInstruction(String systemInstruction, String userPrompt, String model) {
		String modelName = (model != null && !model.isEmpty()) ? model : "gemini-3-flash-preview";

		GenerateContentConfig config = GenerateContentConfig.builder()
				.systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
				.build();

		GenerateContentResponse response = client.models.generateContent(modelName,
				userPrompt, config);

		return response.text();
	}

	/**
	 * System Instruction을 사용하여 텍스트를 생성합니다 (기본 모델 사용).
	 * 
	 * @param systemInstruction 모델의 역할과 동작 방식을 정의하는 지시사항
	 * @param userPrompt        실제 사용자의 질문이나 요청
	 * @return 생성된 텍스트 응답
	 */
	public String systemInstruction(String systemInstruction, String userPrompt) {
		return systemInstruction(systemInstruction, userPrompt, "gemini-3-flash-preview");
	}

	public void streaming(String systemInstruction, String userPrompt, String model) {
		String modelName = (model != null && !model.isEmpty()) ? model : "gemini-3-flash-preview";

		GenerateContentConfig config = null;
		if (systemInstruction != null && !systemInstruction.isEmpty()) {
			config = GenerateContentConfig.builder()
					.systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
					.build();
		}

		ResponseStream<GenerateContentResponse> responseStream = client.models.generateContentStream(
				modelName, userPrompt, config);

		for (GenerateContentResponse res : responseStream) {
			System.out.print(res.text());
		}
		responseStream.close();
	}

	/**
	 * System Instruction을 사용하여 스트리밍 텍스트를 생성합니다.
	 * SseEmitter를 통해 실시간으로 응답을 전송합니다.
	 * 
	 * Phase 3: Spring AI Tool 자동 호출 지원
	 * Spring AI ChatModel을 사용하여 Tool 자동 호출 기능을 활용합니다.
	 * 
	 * @param systemInstruction 모델의 역할과 동작 방식을 정의하는 지시사항
	 * @param userPrompt        실제 사용자의 질문이나 요청
	 * @param model             사용할 모델명 (선택사항, 무시됨 - ChatModel의 기본 모델 사용)
	 * @return SseEmitter 스트리밍 응답을 위한 emitter
	 */
	public SseEmitter streamingSse(String systemInstruction, String userPrompt, String model) {
		log.info("GeminiTextService: 스트리밍 SSE 요청 - systemInstruction: {}, userPrompt: {}",
				systemInstruction, userPrompt);

		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 타임아웃 없음

		CompletableFuture.runAsync(() -> {
			try {
				// Phase 3: Spring AI ChatModel을 사용하여 Tool 자동 호출 지원
				// Spring AI가 자동으로 Tool을 호출하고 결과를 LLM에 전달합니다.

				// 메시지 준비
				List<Message> messages = new ArrayList<>();
				if (systemInstruction != null && !systemInstruction.isEmpty()) {
					messages.add(new SystemMessage(systemInstruction));
				}
				messages.add(new UserMessage(userPrompt));

				// Spring AI ChatModel이 StreamingChatModel을 구현하는지 확인
				if (chatModel instanceof StreamingChatModel) {
					// 스트리밍 지원
					StreamingChatModel streamingChatModel = (StreamingChatModel) chatModel;
					Prompt prompt = new Prompt(messages);

					Flux<ChatResponse> responseFlux = streamingChatModel.stream(prompt);

					responseFlux.doOnError(error -> {
						log.error("스트리밍 중 오류 발생", error);
						emitter.completeWithError(error);
					})
							.doOnComplete(() -> {
								try {
									emitter.send(SseEmitter.event()
											.name("complete")
											.data("스트리밍 완료"));
									emitter.complete();
								} catch (IOException e) {
									log.error("스트리밍 완료 이벤트 전송 중 오류", e);
									emitter.completeWithError(e);
								}
							})
							.subscribe(chatResponse -> {
								try {
									String content = chatResponse.getResult().getOutput().getText();
									if (content != null && !content.isEmpty()) {
										emitter.send(SseEmitter.event()
												.name("message")
												.data(content));
									}
								} catch (IOException e) {
									log.error("스트리밍 메시지 전송 중 오류", e);
									emitter.completeWithError(e);
								}
							});
				} else {
					// 스트리밍 미지원 - 일반 호출 사용
					log.warn("ChatModel이 StreamingChatModel을 구현하지 않음. 일반 호출 사용");
					Prompt prompt = new Prompt(messages);
					ChatResponse response = chatModel.call(prompt);

					String content = response.getResult().getOutput().getText();
					if (content != null && !content.isEmpty()) {
						emitter.send(SseEmitter.event()
								.name("message")
								.data(content));
					}

					emitter.send(SseEmitter.event()
							.name("complete")
							.data("완료"));
					emitter.complete();
				}
			} catch (Exception e) {
				log.error("스트리밍 중 오류 발생", e);
				emitter.completeWithError(e);
			}
		});

		return emitter;
	}
}
