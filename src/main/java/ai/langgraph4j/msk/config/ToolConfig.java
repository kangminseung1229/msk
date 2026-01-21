package ai.langgraph4j.msk.config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ai.langgraph4j.msk.tools.CalculatorTool;
import ai.langgraph4j.msk.tools.SearchTool;
import ai.langgraph4j.msk.tools.WeatherTool;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Spring AI Tool 설정 클래스
 * Phase 3: Tool을 Spring AI ToolCallback으로 등록합니다.
 * 
 * @Tool 어노테이션이 적용된 Tool 클래스를 MethodToolCallback으로 변환하여
 *       Spring AI가 자동으로 Tool 호출을 처리할 수 있도록 합니다.
 */
@Configuration
@org.springframework.context.annotation.Profile("!test")
@RequiredArgsConstructor
public class ToolConfig {

	private final CalculatorTool calculatorTool;
	private final SearchTool searchTool;
	private final WeatherTool weatherTool;

	/**
	 * 모든 Tool을 ToolCallback 리스트로 등록
	 * 
	 * @Tool 어노테이션이 적용된 메서드들을 MethodToolCallback으로 변환합니다.
	 */
	@Bean
	@SneakyThrows
	public List<ToolCallback> toolCallbacks() {
		List<ToolCallback> callbacks = new ArrayList<>();

		// CalculatorTool의 calculate 메서드
		Method calculateMethod = CalculatorTool.class.getMethod("calculate", String.class);
		callbacks.add(MethodToolCallback.builder()
				.toolDefinition(ToolDefinitions.builder(calculateMethod).build())
				.toolMethod(calculateMethod)
				.toolObject(calculatorTool)
				.build());

		// SearchTool의 search 메서드
		Method searchMethod = SearchTool.class.getMethod("search", String.class);
		callbacks.add(MethodToolCallback.builder()
				.toolDefinition(ToolDefinitions.builder(searchMethod).build())
				.toolMethod(searchMethod)
				.toolObject(searchTool)
				.build());

		// WeatherTool의 getWeather 메서드
		Method getWeatherMethod = WeatherTool.class.getMethod("getWeather", String.class);
		callbacks.add(MethodToolCallback.builder()
				.toolDefinition(ToolDefinitions.builder(getWeatherMethod).build())
				.toolMethod(getWeatherMethod)
				.toolObject(weatherTool)
				.build());

		return callbacks;
	}
}
