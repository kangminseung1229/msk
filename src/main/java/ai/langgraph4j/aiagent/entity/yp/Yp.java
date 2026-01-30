package ai.langgraph4j.aiagent.entity.yp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * yp : 예판
 */

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Table(indexes = @Index(name = "idx_document_number", columnList = "documentNumber"))
public class Yp {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_YP")
  @SequenceGenerator(name = "SEQ_YP", sequenceName = "SEQ_YP")
  private Long id;

  @Size(max = 10)
  private String taxLawCodeLawId;

  // 등록 유형
  // @Size(max = 100)
  @Enumerated(EnumType.STRING)
  private RegistType registType;

  // 문선 번호
  @Size(max = 100)
  private String documentNumber;

  private String documentDate;

  // 제목
  @Size(max = 4000)
  private String title;

  // 예판 구분
  @Enumerated(EnumType.STRING)
  private YpGubun ypGubun;

  // 예판 유형
  @Enumerated(EnumType.STRING)
  private YpType ypType;

  // 메인 여부
  @Builder.Default
  private boolean mainYn = false;

  // 내용
  @Column(columnDefinition = "TEXT")
  private String content;

  // 요약
  @Column(columnDefinition = "TEXT")
  private String summary;

  // 등록 일시
  @CreationTimestamp
  @Column(updatable = false)
  @JsonIgnore
  private LocalDateTime registAt;

  // 등록 아이디
  private Long registId;

  // 수정 일시
  @UpdateTimestamp
  @JsonIgnore
  private LocalDateTime updateAt;

  // 수정 아이디
  private Long updateId;

  // 전문가 추천
  @Builder.Default
  private boolean recommendYn = false;

  @Builder.Default
  private long hits = 0;

  private Integer score;

  // 삭제 여부
  @Builder.Default
  private boolean deleteYn = false;

  private LocalDate deleteDate;

}