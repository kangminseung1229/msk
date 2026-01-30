package ai.langgraph4j.aiagent.entity.yp;
	

public enum YpGubun {

	YG0010("사전답변"),
	YG0020("질의회신"),
	YG0030("과세자문"),
	YG0040("심사청구"),
	YG0050("심판청구"),
	YG0060("판례"),
	YG0070("헌재");
    
    private final String displayValue;

    private YpGubun(String displayValue){
        this.displayValue = displayValue;
    }

    public String getDisplayValue(){
        return displayValue;
    }

}
