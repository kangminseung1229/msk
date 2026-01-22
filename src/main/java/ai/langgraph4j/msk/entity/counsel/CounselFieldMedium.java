package ai.langgraph4j.msk.entity.counsel;


import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
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
public class CounselFieldMedium {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "seq_counsel_field_medium"
    )
    @SequenceGenerator(
        name = "seq_counsel_field_medium"
    )
    
    private Long id;

    private String name;
    private int orderer; // 순서
    
    @ManyToMany(cascade = CascadeType.ALL)
    private List<CounselFieldSmall> counselFieldSmall;


    @Builder.Default
    private boolean useYn = true; // 사용여부


    @CreationTimestamp
    @Column(updatable=false)
    private LocalDateTime createAt; // 상담일시


    @UpdateTimestamp
    private LocalDateTime updateAt; // 수정일시
}
