package ai.langgraph4j.aiagent.entity.law;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * LawBasicInformation : 법령 기본 정보
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class LawBasicInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lawBasicInformation_SEQ_GENERATOR")
    @SequenceGenerator(name = "lawBasicInformation_SEQ_GENERATOR", sequenceName = "lawBasicInformation_SEQ", allocationSize = 100)
    private Long id;

    @Column(length = 32, unique = true)
    private String lawKey; // 법령키

    @Column(length = 8)
    private String enforceDate; // 시행일자

    private String lawNameKorean;// 법령명_한글

    private String lawNameNickname;// 법령명 약칭

    @Column(length = 10)
    private String mst; // 법령일련번호

    @Column(length = 10)
    private String lawId; // 법령ID

    // 공포일자
    @Column(length = 8)
    private String proclamationDate;

    // 공포번호
    @Column(length = 10)
    private String proclamationNumber;

    // 소관부처
    private String ministry;

    // 개정구분
    @Size(max = 10)
    private String modify;

    @Column(length = 10)
    private String lawCategoryCode; // 법종 구분 코드

    @Column(length = 50)
    private String lawCategory; // 법종 구분 이름

    // 조문시행일자문자열
    @Column(columnDefinition = "TEXT")
    private String applicationDate;

    @CreationTimestamp
    @Column(updatable = false)
    @JsonIgnore
    private LocalDateTime registAt;

    @UpdateTimestamp
    @JsonIgnore
    private LocalDateTime updateAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private DataStatus dataStatus;

    @OneToMany(mappedBy = "lawBasicInformation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Article> articles; // 조문

    @OneToOne(mappedBy = "lawBasicInformation", cascade = CascadeType.ALL)
    @JsonManagedReference
    private AmendmentReason amendmentReason;

    @OneToOne(mappedBy = "lawBasicInformation", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Amendment amendment;

    @OneToMany(mappedBy = "lawBasicInformation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Addendum> addendum; // 부칙

    @OneToMany(mappedBy = "lawBasicInformation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Attachedform> attachedform;

    @OneToMany(mappedBy = "lawBasicInformation", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<RecentAddendum> recentAddendums = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String attachedFormEnforceDate; // 별표시행 문자열

    /**
     * 공포일자 포맷
     * @return
     */
    public String proclamationDateFormat() {
        if (this.proclamationDate== null || this.proclamationDate.length() != 8) {
            throw new IllegalArgumentException("날짜 형식은 반드시 8자리 (yyyyMMdd)여야 합니다.");
        }

        String year = this.proclamationDate.substring(0, 4);
        String month = this.proclamationDate.substring(4, 6);
        String day = this.proclamationDate.substring(6, 8);

        return year + "." + month + "." + day;
    }
    /**
     * 시행일자 포맷
     * @return
     */
    public String enforceDateFormat() {
        if (this.enforceDate== null || this.enforceDate.length() != 8) {
            throw new IllegalArgumentException("날짜 형식은 반드시 8자리 (yyyyMMdd)여야 합니다.");
        }

        String year = this.enforceDate.substring(0, 4);
        String month = this.enforceDate.substring(4, 6);
        String day = this.enforceDate.substring(6, 8);

        return year + "." + month + "." + day;
    }

}