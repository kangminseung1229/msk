package ai.langgraph4j.aiagent.entity.yp;


public enum RegistType {

    YT0010("세무"),
    YT0020("세무/일반"),
    YT0030("일반"),
    YT0040("노무"),
    YT0050("행정해석");

    private final String displayValue;

    private RegistType(String displayValue){
        this.displayValue = displayValue;
    }

    public String getDisplayValue(){
        return displayValue;
    }

}