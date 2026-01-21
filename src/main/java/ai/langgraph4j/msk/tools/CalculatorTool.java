package ai.langgraph4j.msk.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.parser.ParseException;

import lombok.extern.slf4j.Slf4j;

/**
 * 계산기 도구
 * AI 에이전트가 수학 계산을 수행할 수 있도록 하는 도구입니다.
 * 
 * EvalEx 라이브러리를 사용하여 복잡한 수학 표현식을 안전하게 평가합니다.
 * 
 * 지원 기능:
 * - 기본 사칙연산 (+, -, *, /)
 * - 연산자 우선순위 지원
 * - 괄호 지원
 * - 수학 함수 (sin, cos, sqrt, log, pow 등)
 * - 세금 계산 등 복잡한 계산 지원
 * 
 * Phase 3에서 Spring AI Tool 인터페이스로 통합 예정
 */
@Slf4j
@Component
public class CalculatorTool {

	/**
	 * 수학 표현식을 계산합니다.
	 * 
	 * @param expression 수학 표현식 (예: "123 + 456", "10 * 5", "(1 + 2) * 3", "sqrt(16)")
	 * @return 계산 결과 (문자열)
	 */
	@Tool(description = "수학 표현식을 계산합니다. 기본 사칙연산, 괄호, 수학 함수(sin, cos, sqrt, log, pow 등)를 지원합니다.")
	public String calculate(
			@ToolParam(description = "계산할 수학 표현식 (예: '123 + 456', '10 * 5', '(1 + 2) * 3', 'sqrt(16)')") String expression) {
		if (expression == null || expression.trim().isEmpty()) {
			return "오류: 계산할 표현식이 비어있습니다.";
		}

		try {
			expression = expression.trim();
			log.debug("CalculatorTool: 계산 시작 - {}", expression);

			// EvalEx를 사용하여 표현식 평가
			Expression evalExpression = new Expression(expression);
			EvaluationValue evaluationValue = evalExpression.evaluate();
			BigDecimal result = evaluationValue.getNumberValue();

			// 결과 포맷팅 (소수점 10자리까지, 불필요한 0 제거)
			String resultStr = result.setScale(10, RoundingMode.HALF_UP)
					.stripTrailingZeros()
					.toPlainString();

			log.debug("CalculatorTool: 계산 완료 - {} = {}", expression, resultStr);
			return resultStr;

		} catch (ParseException e) {
			log.error("CalculatorTool: 표현식 파싱 오류 - {}", expression, e);
			return "오류: 표현식을 파싱할 수 없습니다. " + e.getMessage();
		} catch (EvaluationException e) {
			log.error("CalculatorTool: 계산 평가 오류 - {}", expression, e);
			return "오류: 계산 중 오류가 발생했습니다. " + e.getMessage();
		} catch (Exception e) {
			log.error("CalculatorTool: 예상치 못한 오류 - {}", expression, e);
			return "오류: 계산 중 예상치 못한 오류가 발생했습니다. " + e.getMessage();
		}
	}

	/**
	 * 세금 계산을 수행합니다.
	 * 
	 * @param amount 금액
	 * @param taxRate 세율 (백분율, 예: 10 = 10%)
	 * @return 세금 포함 금액
	 */
	public String calculateWithTax(String amount, String taxRate) {
		try {
			BigDecimal amountDecimal = new BigDecimal(amount);
			BigDecimal rateDecimal = new BigDecimal(taxRate);

			// 세금 계산: 금액 * (1 + 세율/100)
			BigDecimal taxMultiplier = rateDecimal.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
					.add(BigDecimal.ONE);
			BigDecimal result = amountDecimal.multiply(taxMultiplier);

			String resultStr = result.setScale(2, RoundingMode.HALF_UP)
					.stripTrailingZeros()
					.toPlainString();

			log.debug("CalculatorTool: 세금 계산 완료 - 금액: {}, 세율: {}%, 결과: {}", amount, taxRate, resultStr);
			return resultStr;

		} catch (Exception e) {
			log.error("CalculatorTool: 세금 계산 오류 - 금액: {}, 세율: {}", amount, taxRate, e);
			return "오류: 세금 계산 중 오류가 발생했습니다. " + e.getMessage();
		}
	}

	/**
	 * 백분율 계산을 수행합니다.
	 * 
	 * @param value 값
	 * @param percentage 백분율 (예: 10 = 10%)
	 * @return 계산 결과
	 */
	public String calculatePercentage(String value, String percentage) {
		try {
			BigDecimal valueDecimal = new BigDecimal(value);
			BigDecimal percentageDecimal = new BigDecimal(percentage);

			// 백분율 계산: 값 * (백분율/100)
			BigDecimal result = valueDecimal.multiply(percentageDecimal)
					.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);

			String resultStr = result.setScale(10, RoundingMode.HALF_UP)
					.stripTrailingZeros()
					.toPlainString();

			log.debug("CalculatorTool: 백분율 계산 완료 - 값: {}, 백분율: {}%, 결과: {}", value, percentage, resultStr);
			return resultStr;

		} catch (Exception e) {
			log.error("CalculatorTool: 백분율 계산 오류 - 값: {}, 백분율: {}", value, percentage, e);
			return "오류: 백분율 계산 중 오류가 발생했습니다. " + e.getMessage();
		}
	}
}
