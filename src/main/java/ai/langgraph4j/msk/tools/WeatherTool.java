package ai.langgraph4j.msk.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * 날씨 도구
 * AI 에이전트가 날씨 정보를 조회할 수 있도록 하는 도구입니다.
 * 
 * Phase 2에서는 간단한 구조로 구현하며,
 * Phase 3에서 실제 날씨 API (OpenWeatherMap API, WeatherAPI 등) 통합 예정입니다.
 * 
 * 지원 기능:
 * - 도시별 날씨 조회
 * - 날씨 정보 요약
 * 
 * 참고: 현재는 구조만 구현되어 있으며, 실제 날씨 API 통합은 Phase 3에서 진행합니다.
 */
@Slf4j
@Component
public class WeatherTool {

	// Phase 3에서 실제 날씨 API 통합 시 사용 예정
	@SuppressWarnings("unused")
	private final RestTemplate restTemplate;

	public WeatherTool() {
		this.restTemplate = new RestTemplate();
	}

	/**
	 * 도시의 날씨 정보를 조회합니다.
	 * 
	 * @param location 도시명 또는 위치 (예: "서울", "Seoul", "서울, 한국")
	 * @return 날씨 정보 (문자열)
	 */
	@Tool(description = "도시의 날씨 정보를 조회합니다. 도시명 또는 위치 정보를 입력받아 날씨 정보를 반환합니다.")
	public String getWeather(
			@ToolParam(description = "도시명 또는 위치 (예: '서울', 'Seoul', '서울, 한국')") String location) {
		if (location == null || location.trim().isEmpty()) {
			return "오류: 위치 정보가 비어있습니다.";
		}

		try {
			location = location.trim();
			log.debug("WeatherTool: 날씨 조회 시작 - {}", location);

			// Phase 2: 간단한 응답 반환
			// Phase 3에서 실제 날씨 API 통합 예정
			// 예: OpenWeatherMap API, WeatherAPI 등
			
			// 현재는 위치를 기반으로 간단한 응답 생성
			String result = performWeatherQuery(location);
			
			log.debug("WeatherTool: 날씨 조회 완료 - {}", location);
			return result;

		} catch (RestClientException e) {
			log.error("WeatherTool: 날씨 API 호출 오류 - {}", location, e);
			return "오류: 날씨 API 호출 중 오류가 발생했습니다. " + e.getMessage();
		} catch (Exception e) {
			log.error("WeatherTool: 예상치 못한 오류 - {}", location, e);
			return "오류: 날씨 조회 중 예상치 못한 오류가 발생했습니다. " + e.getMessage();
		}
	}

	/**
	 * 실제 날씨 조회 수행
	 * 
	 * Phase 2: 간단한 응답 반환
	 * Phase 3: 실제 날씨 API 통합
	 * 
	 * @param location 위치 정보
	 * @return 날씨 정보
	 */
	private String performWeatherQuery(String location) {
		// Phase 2: 간단한 응답
		// 실제 날씨 API가 없으므로 위치를 기반으로 한 간단한 응답 반환
		// Phase 3에서 실제 API 통합 예정
		
		// 예시: OpenWeatherMap API 사용 시
		// String apiKey = "your-api-key";
		// String url = String.format(
		//     "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=kr",
		//     URLEncoder.encode(location, StandardCharsets.UTF_8), apiKey);
		// 
		// ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		// return parseWeatherData(response.getBody());
		
		// 현재는 위치를 기반으로 한 안내 메시지 반환
		return String.format(
			"%s의 날씨 정보:\n" +
			"[Phase 2: 날씨 기능은 구조만 구현되어 있습니다. Phase 3에서 실제 날씨 API를 통합할 예정입니다.]\n" +
			"위치: %s\n" +
			"실제 날씨 API 통합 후 정확한 날씨 정보(온도, 습도, 날씨 상태 등)를 제공할 수 있습니다.",
			location, location
		);
	}

	/**
	 * 날씨 정보 요약
	 * 
	 * @param location 위치 정보
	 * @return 요약된 날씨 정보
	 */
	public String getWeatherSummary(String location) {
		if (location == null || location.trim().isEmpty()) {
			return "오류: 위치 정보가 비어있습니다.";
		}

		try {
			location = location.trim();
			log.debug("WeatherTool: 날씨 요약 시작 - {}", location);

			// Phase 2: 간단한 응답
			String fullResult = getWeather(location);
			
			// 요약 버전 반환 (실제로는 날씨 정보를 요약)
			String summary = String.format(
				"%s 날씨 요약:\n%s",
				location, fullResult
			);
			
			log.debug("WeatherTool: 날씨 요약 완료 - {}", location);
			return summary;

		} catch (Exception e) {
			log.error("WeatherTool: 날씨 요약 오류 - {}", location, e);
			return "오류: 날씨 요약 중 오류가 발생했습니다. " + e.getMessage();
		}
	}
}
