package ai.langgraph4j.msk.entity.counsel;

public enum CounselorTypeCode {

    CG0010("세무사"),
    CG0020("회계사"),
    CG0030("변호사"),
    CG0040("노무사"),
    CG0050("상담위원"),
    CG0060("관리자");
    	
    private final String displayValue;

    private CounselorTypeCode(String displayValue){
        this.displayValue = displayValue;
    }

    public String getDisplayValue(){
        return displayValue;
    }

}