package ai.langgraph4j.msk.entity.counsel;
import lombok.Getter;

@Getter
public enum CounselStatus {

    WAIT("답변대기"),
    PROGRESS("진행중"),
    RESERVATION_COMPLETE("예약완료"),
    COMPLETE("답변완료");
    
    private final String displayValue;

    private CounselStatus(String displayValue){
        this.displayValue = displayValue;
    }

}
