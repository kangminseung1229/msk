package ai.langgraph4j.aiagent.entity.law;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 세무법 표현 테이블
 */
@Entity
@Getter
@Setter
public class TaxLawCode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_tax_law_code")
    @SequenceGenerator(name = "seq_tax_law_code")
    private Long id;

    private String lawNameKorean;

    @Size(max = 10)
    @Column(unique = true)
    private String lawId;

    private int orderer; // 순서

    private String lawNickName; // 별칭

    private String lawGroupName; // 그룹

    private Boolean lawMain; // 법률여부

}
