package ai.langgraph4j.msk.service;

import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.ThinkingLevel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google Gemini API를 사용한 텍스트 생성 서비스
 * 샘플 코드를 기반으로 작성된 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiTextService {

	private final Client client;

	/**
	 * 텍스트 입력을 받아 Gemini API로 응답을 생성합니다.
	 * 
	 * @param prompt 사용자 입력 프롬프트
	 * @param model  사용할 모델명 (기본값: gemini-3-flash-preview)
	 * @return 생성된 텍스트 응답
	 */
	public String generateText(String prompt, String model) {
		log.info("GeminiTextService: 텍스트 생성 요청 - 모델: {}, 프롬프트: {}", model, prompt);

		try {
			String modelName = (model != null && !model.isEmpty()) ? model : "gemini-3-flash-preview";

			GenerateContentResponse response = client.models.generateContent(
					modelName,
					prompt,
					null);

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
		return generateText(prompt, "gemini-3-flash-preview");
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
}
