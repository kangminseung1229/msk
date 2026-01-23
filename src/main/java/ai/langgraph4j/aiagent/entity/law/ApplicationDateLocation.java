package ai.langgraph4j.aiagent.entity.law;


import com.fasterxml.jackson.annotation.JsonIgnore;

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
public class ApplicationDateLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_applicaition_date_location")
    @SequenceGenerator(name = "seq_applicaition_date_location", sequenceName = "seq_applicaition_date_location")
    @JsonIgnore
    private Long id;

    @Size(max = 8)
    private String articleCode; // 조문코드

    @Size(max = 4)
    private String paragraph; // 항

    @Size(max = 3)
    private String subParagraph; // 호

    @Size(max = 2)
    private String item; // 목


}
