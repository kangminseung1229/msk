package ai.langgraph4j.msk.entity.counsel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import ai.langgraph4j.msk.entity.law.LawArticleCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 상담
 */
@Data
@Entity
@EqualsAndHashCode(of = "id")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Counsel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private Counsel parentCounsel; // 이전 상담 (부모상담)

    @Builder.Default
    private boolean recommendTargetYn = false; // 추천 대상 여부

    @Builder.Default
    private boolean recommendExceptYn = false; // 추천 제외 여부

    @Builder.Default
    private boolean recommendAddYn = false; // 추천 추가 여부

    private String counseleeName; // 상담자명

    private String email; // 이메일
    private String mobile; // 상담 받을 때 변경할 휴대폰

    @Builder.Default
    private boolean smsReceiveYn = false; // SMS 수신 여부

    private String companyName; // 회사 명

    @ManyToOne
    private CounselFieldLarge counselFieldLarge; // 상담 분야 대분류 아이디 (FK)

    @ManyToOne
    private CounselFieldMedium counselFieldMedium; // 상담 분야 중분류 아이디 (FK)

    @ManyToOne
    private CounselFieldSmall counselFieldSmall; // 상담 분양 소분류 아이디 (FK)

    private String counselTitle; // 상담 제목

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String counselContent; // 상담 내용

    // @NotNull
    @Column(columnDefinition = "TEXT")
    private String counselRelationContent; // 상담 관련 내용

    private String answerTitle; // 답변 제목

    @Column(columnDefinition = "TEXT")
    private String answerContent; // 답변 내용

    private String keyword; // 키워드 택스로이드...

    @Max(5)
    private int importance; // 중요도

    @ManyToOne
    private Counselor answerer; // 답변한 상담위원

    @Builder.Default
    private boolean deleted = false; // 삭제여부

    @Enumerated(EnumType.STRING)
    private CounselStatus counselStatus; // 진행상태

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime counselAt; // 상담일시

    private LocalDateTime answerAt; // 답변 일시

    private Long oldId;

    @Builder.Default
    @ManyToMany
    private List<LawArticleCode> lawArticleCodes = new ArrayList<>();

    private String relationLawString; // 연관법령 문자열 파서의 대상, 노출할 때 사용

    @UpdateTimestamp
    private LocalDateTime updateAt; // 수정 일시

    private LocalDateTime answerTimeLimit; // 답변 시간 제한

}
