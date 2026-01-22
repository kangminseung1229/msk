package ai.langgraph4j.msk.entity.counsel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Counselor {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_counselor")
    @SequenceGenerator(name = "seq_counselor")
    private Long id;

    @Builder.Default
    private boolean activeYn = false; // 활동 여부

    @Builder.Default
    private boolean bestYn = false; // BEST 여부

    @Builder.Default
    private boolean videoYn = false; // 화상 여부

    @Builder.Default
    private boolean expertYn = false; // 전문가 여부

    @Builder.Default
    @ManyToMany(cascade = CascadeType.ALL)
    private List<CounselFieldLarge> representCounselCode = new ArrayList<>(); // 대표 상담분야

    @Builder.Default
    @ManyToMany(cascade = CascadeType.ALL)
    private List<CounselFieldLarge> reConsultingCounselCode = new ArrayList<>(); // 그냥 상담분야

    @Builder.Default
    @ManyToMany(cascade = CascadeType.ALL)
    private List<CounselFieldLarge> reEtcCounselCode = new ArrayList<>(); // 기타 상담분야

    @Builder.Default
    private boolean representSmsYn = false; // 대표 SMS 여부

    @Builder.Default
    private boolean etcSmsYn = false; // 기타 SMS 여부

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private CounselorTypeCode counselorTypeCode; // 분류

    private String phone; // 연락처

    private String email; // 이메일

    @Size(max = 2000)
    private String academy; // 학력

    @Size(max = 2000)
    private String career; // 경력

    @Size(max = 2000)
    private String lecture; // 강의

    @Column(columnDefinition = "TEXT")
    private String greeting; // 인사말

    @Builder.Default
    private boolean deleted = false; // 삭제여부

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private Long oldId;

}
