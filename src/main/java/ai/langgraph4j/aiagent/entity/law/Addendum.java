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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Addendum : 부칙
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Builder
public class Addendum {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ADDENDUM_SEQ_GENERATOR")
    @SequenceGenerator(name = "ADDENDUM_SEQ_GENERATOR", sequenceName = "ADDENDUM_SEQ", allocationSize = 100)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String addendumContent; // 부칙 내용

    @Column(length = 10)
    private String addendumProclamationDate; // 공포일자

    @Column(length = 10)
    private String addendumProclamationNumber; // 공포번호

    @CreationTimestamp
    @Column(updatable = false)
    @JsonIgnore
    private LocalDateTime registAt;

    @UpdateTimestamp
    @JsonIgnore
    private LocalDateTime updateAt;

    @ManyToOne
    @JsonBackReference
    private LawBasicInformation lawBasicInformation;

}