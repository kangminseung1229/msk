package ai.langgraph4j.aiagent.entity.law;

/*
 * 데이터 상태 구분 값
 */
public enum DataStatus {
    WAIT("대기중"),
    PARSER("파서적용완료"),
    TAXNET("택스넷이관완료");

    private final String displayValue;

    private DataStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}
