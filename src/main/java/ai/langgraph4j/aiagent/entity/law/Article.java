package ai.langgraph4j.aiagent.entity.law;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * article : 조문
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Table(indexes = @Index(name = "article_law_basic_information_id_idx", columnList = "law_basic_information_id, articleKey"))
public class Article implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "article_SEQ_GENERATOR")
    @SequenceGenerator(name = "article_SEQ_GENERATOR", sequenceName = "article_SEQ", allocationSize = 100)
    @JsonIgnore
    private Long id;

    @Column(length = 5)
    private String articleNumber; // 조문번호

    @Column(length = 5)
    private String articleBranchNumber; // 조문가지번호

    @Column(length = 10)
    private String articleEnforceDate; // 조문시행일자

    private String articleTitle; // 조문제목

    @Column(columnDefinition = "TEXT")
    private String articleOriginalContent; // 조문내용

    @Column(columnDefinition = "TEXT")
    private String articleLinkContent; // 조문내용

    @Column(length = 2)
    private String articleYn; // 조문여부

    @Column(length = 2)
    private String articleChangeYn; // 조문변경여부

    @Column(columnDefinition = "TEXT")
    private String articleReference; // 조문참고자료

    @CreationTimestamp
    @Column(updatable = false)
    @JsonIgnore
    private LocalDateTime registAt;

    @UpdateTimestamp
    @JsonIgnore
    private LocalDateTime updateAt;

    private String articleKey; // 조문키

    @ManyToOne
    @JsonBackReference
    private LawBasicInformation lawBasicInformation;

    private boolean parserSuccessYn = false; // 파서 성공 여부
    private boolean independentEnforceDate = false; // 적용시기 여부

    /**
     * article code 만들기. 몇조의 몇 -> 몇-몇
     *
     * @return
     */
    public String getArticleCode() {

        if (this.articleBranchNumber == null) {
            return this.articleNumber;
        } else {
            return this.articleNumber + "-" + this.articleBranchNumber;
        }

    }

    public String getKoreanString() {
        if (this.articleBranchNumber == null) {
            return "제" + this.articleNumber + "조";
        } else {
            return "제" + this.articleNumber + "조의" + this.articleBranchNumber;
        }
    }

    /**
     * 0018031 -> 18-3
     *
     * @param articleKey
     * @return
     */
    public static String makeArticleCode(String articleKey) {

        String number = articleKey.substring(0, 4);
        String branchNubmer = articleKey.substring(4, 6);

        return branchNubmer.equals("00") ? String.valueOf(Integer.parseInt(number))
                : String.valueOf(Integer.parseInt(number)) + "-" + String.valueOf(Integer.parseInt(branchNubmer));

    }

    /**
     * ht
     * 0015021 -> 15조의 2
     *
     * @param articleKey
     * @return
     */
    public static String keyToKoreanCode(String articleKey) {

        Integer number;
        Integer branchNubmer;

        try {
            number = Integer.valueOf(articleKey.substring(0, 4));
            branchNubmer = Integer.valueOf(articleKey.substring(4, 6));

        } catch (StringIndexOutOfBoundsException e) {
            e.printStackTrace();
            return "";
        }

        return branchNubmer == 0 ? number + "조" : number + "조의 " + branchNubmer;
    }

    /**
     * 셀렉트 박스 조문 제목
     *
     * @return
     */
    public String articleSelectBoxTitle() {
        return getKoreanString() + " " + this.articleTitle;
    }

    /**
     * 18-3 => 0018031
     *
     * @param input
     * @return
     */
    public static String generateClauseKey(String input) {
        // 입력 형식: "18-3"
        String[] parts = input.split("-");

        // 조문번호 (앞 4자리)
        String clauseNumber = String.format("%04d", Integer.parseInt(parts[0]));

        // 가지번호 (뒤 2자리)
        String branchNumber = input.contains("-") ? String.format("%02d", Integer.parseInt(parts[1])) : "00";

        // 고정값 "1" 추가
        return clauseNumber + branchNumber + "1";
    }

    /**
     * 조문 타이틀 컬럼이 공백이면 삭제 된 조문
     * 삭제 여부를 리턴함.
     *
     * @return
     */
    public boolean isDeleteArticle() {
        return ObjectUtils.isEmpty(this.articleTitle);
    }

    // 이미지 설명 부분 날리기...
    public String removeArticleImageDescription(String articleContents) {
        String regex = "<img[^>]*>(.*?)</img>";
        String returnStr = articleContents;
        // 일단 조문의 null부터 지우고
        returnStr = returnStr.replaceAll("null", "");
        // 이미지 태그와 아래 글 사이 간격이 너무 좁아보인다.
        returnStr = returnStr.replaceAll("<img", "<br/> <img");
        returnStr = returnStr.replaceAll("</img>", "</img> <br/>");

        // img태그를 찾자
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(articleContents);
        while (matcher.find()) {
            if (matcher.groupCount() == 1) { // parentPattern.capturingGroupCount - 1
                String matchStr = matcher.group(1);
                returnStr = returnStr.replace(matchStr, "");
            }
        }
        return returnStr;
    }

}