package ai.langgraph4j.aiagent.entity.law;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 법이나 서식의 데이터 개정여부 판단
 */
@AllArgsConstructor
@Getter
public enum AmendmentType {
        AMENDMENT("개정"),
        ENACTMENT("신설"),
        REPEAL("삭제"),
        NONE("알수없음");

    private String displayValue;
}
