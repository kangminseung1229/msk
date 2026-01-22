package ai.langgraph4j.msk.entity.law;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class LawArticleCode {

  // 아이디
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAW_ARTICLE_CODE")
  @SequenceGenerator(name = "SEQ_LAW_ARTICLE_CODE")
  @JsonIgnore
  private Long id;

  // 법령 아이디
  @Size(max = 19)
  private String lawId;

  // 조문 키
  @Size(max = 29)
  private String articleKey;

  // 등록록일시
  @CreationTimestamp
  @Column(updatable = false)
  @JsonIgnore
  private LocalDateTime registAt;

  // 등록 아이디
  private Long registId;

  public String convertToDashFormat(){

    String code = this.articleKey;

    // 첫 4자리 숫자를 읽고, 앞의 0을 제거
    String mainPart = String.valueOf(Integer.parseInt(code.substring(0, 4)));

    // 마지막 3자리 중 뒤 2자리를 숫자로 변환
    int lastPart = Integer.parseInt(code.substring(code.length() - 2));

    // 마지막 2자리가 '01'인 경우 변환 없이 반환
    if (lastPart == 1) {
      return mainPart;
    } else {
      return mainPart + "-" + lastPart;
    }
  }


  /**
   * 몇조의몇
   * @return
   */
  public static String convertToKoreanFormat(String code){

    // 첫 4자리 숫자를 읽고, 앞의 0을 제거
    String mainPart = String.valueOf(Integer.parseInt(code.substring(0, 4)));

    // 마지막 3자리 중 뒤 2자리를 숫자로 변환
    int lastPart = Integer.parseInt(code.substring(code.length() - 2));

    // 마지막 2자리가 '01'인 경우 변환 없이 반환
    if (lastPart == 1) {
      return "제 " + mainPart +"조";
    } else {
      return "제 " + mainPart +"조의 " + lastPart;
    }
  }
}