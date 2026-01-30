package ai.langgraph4j.aiagent.entity.yp;
	

public enum YpType {

	YB0010("인용"),
	YB0020("일부인용"),
	YB0030("경정"),
	YB0040("취소"),
	YB0050("재조사"),
	YB0060("국패"),
	YB0070("국승"),
	YB0080("일부국패"),
	YB0090("일부국승"),
	YB0100("위험"),
	YB0110("합헌"),
	YB0120("헌법불합치"),
	YB0130("기각"),
	YB0140("각하"),
	YB0150("기타"),
	YB0160("결정없음");
    
    private final String displayValue;

    private YpType(String displayValue){
        this.displayValue = displayValue;
    }

    public String getDisplayValue(){
        return displayValue;
    }

}
