package ai.langgraph4j.aiagent.entity.law;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * AmendmentReason : 제개정 이유
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AmendmentReason {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AMENDMENTREASON_SEQ_GENERATOR")
    @SequenceGenerator(name = "AMENDMENTREASON_SEQ_GENERATOR", sequenceName = "AMENDMENTREASON_SEQ", allocationSize = 100)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String amendmentReason; // 제개정이유 텍스트

    @CreationTimestamp
    @Column(updatable = false)
    @JsonIgnore
    private LocalDateTime registAt;

    @UpdateTimestamp
    @JsonIgnore
    private LocalDateTime updateAt;

    @OneToOne
    @JsonBackReference
    private LawBasicInformation lawBasicInformation;

    public String getAmendmentReason() {
        return this.amendmentReason.replaceAll("<br/><br/><br/><br/>", "<br/><br/>");
    }
}