package ai.langgraph4j.aiagent.entity.law;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 최신 부칙 1건 조문 단위 데이터화
 * 적용시기 삽입 데이터 때문에 만들어지게 되었음.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RecentAddendum {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_recentaddendum")
    @SequenceGenerator(name = "seq_recentaddendum", sequenceName = "seq_recentaddendum")
    private Long id;

    @Size(max = 10)
    private String articleNumber;

    @Column(columnDefinition = "TEXT")
    private String title; // 부칙 조문 제목

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JsonBackReference
    private LawBasicInformation lawBasicInformation;

    @ManyToMany(cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<ApplicationDateLocation> applicationDateLocations = new ArrayList<>();
}
