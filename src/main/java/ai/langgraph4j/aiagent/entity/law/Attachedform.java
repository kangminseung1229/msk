package ai.langgraph4j.aiagent.entity.law;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Attachedform : 별표
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Attachedform {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ATTACHEDFORM_SEQ_GENERATOR")
    @SequenceGenerator(name = "ATTACHEDFORM_SEQ_GENERATOR", sequenceName = "ATTACHEDFORM_SEQ", allocationSize = 100)
    private Long id;

    @Size(max = 10)
    private String attachedformKey; // 별표키

    @Size(max = 4)
    private String attachedformNumber; // 별표 번호

    @Size(max = 2)
    private String attachedformBranchNumber; // 별표가지번호

    @Size(max = 4)
    private String attachedformGubun; // 별표 구분

    @Size(max = 500)
    private String attachedformTitle; // 별표 제목

    @Size(max = 10)
    private String attachedformEnforceDate; // 별표시행일자

    private String attachedformHwpLink; // 별표서식파일링크

    private String attachedformHwpFileName; // 별표HWP 파일명

    private String attachedformPdfLink; // 별표서식PDF파일링크

    private String attachedformPdfFileName; // 별표서식PDF파일명

    @Column(columnDefinition = "TEXT")
    private String attachedformContents; // 별표서식 내용

    @Enumerated(EnumType.STRING)
    @Column(length = 9)
    private AmendmentType amendmentType; // 개정여부 (이번 공포호수에서 개정, 신설된것)

    private LocalDate amendmentAt; // 개정,신설,삭제 구분날짜
    private boolean amendment = false; // 개정여부 공포일 +1 시행일자와 같다면 true

    @ManyToOne
    @JsonBackReference
    private LawBasicInformation lawBasicInformation;

    @Builder.Default
    @Column(columnDefinition = "integer default 0")
    private int viewCount = 0;

    /**
     * 서식 노출용 제목
     *
     * @return
     */
    public String title() {

        String title = this.attachedformBranchNumber.equals("00")
                ? String.format("[%s 제 %d호]", this.attachedformGubun, Integer.parseInt(this.attachedformNumber))
                : String.format("[%s 제 %d호의 %d]", this.attachedformGubun, Integer.parseInt(this.attachedformNumber),
                        Integer.parseInt(this.attachedformBranchNumber));

        title += " " + attachedformTitle.replaceAll("<br/>", "")
                + "(" + this.amendmentAt + " " + this.amendmentType.getDisplayValue() + ")";

        return title;
    }

    public String formNumberTitle() {
        String number = this.attachedformNumber.replaceAll("<[^>]+>", "");
        String branch = this.attachedformBranchNumber.replaceAll("<[^>]+>", "");

        boolean isBranchNumeric = branch.matches("\\d+");
        boolean isNumberNumeric = number.matches("\\d+");

        if (isBranchNumeric && branch.equals("00")) {
            return isNumberNumeric
                    ? String.format("%s 제 %d호", this.attachedformGubun, Integer.parseInt(number))
                    : String.format("%s 제 %s호", this.attachedformGubun, number);
        } else if (isBranchNumeric) {
            return isNumberNumeric
                    ? String.format("%s 제 %d호의 %d", this.attachedformGubun,
                            Integer.parseInt(number), Integer.parseInt(branch))
                    : String.format("%s 제 %s호의 %s", this.attachedformGubun, number, branch);
        } else {
            // branch가 숫자가 아닌 경우 문자열 그대로 출력
            return String.format("%s 제 %s호의 %s", this.attachedformGubun, number, branch);
        }
    }

    public String formTitle() {
        String title = "";
        title += " " + attachedformTitle.replaceAll("<br/>", "");
        return title;
    }

    public String lawgokrHwpLink() {
        return "https://www.law.go.kr" + this.attachedformHwpLink;
    }

    public String lawgokrPdfLink() {
        return "https://www.law.go.kr" + this.attachedformPdfLink;
    }

    /**
     * 한글 파일이름
     *
     * @return
     */
    public String encodedFileName() {
        return URLEncoder.encode(this.getAttachedformTitle(), StandardCharsets.UTF_8);
    }

}