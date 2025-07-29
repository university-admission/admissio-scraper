package org.admissio.scraper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Table(name = "applications")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
public class Application {

    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @JsonIgnore
    @NonNull
    @ManyToOne
    @JoinColumn(name = "student_id", referencedColumnName = "id", nullable = false)
    private Student student;

    @JsonIgnore
    @NonNull
    @ManyToOne
    @JoinColumn(name = "offer_id", referencedColumnName = "id", nullable = false)
    private Offer offer;

    @Column(name = "raw_score", nullable = false)
    @NonNull
    private Double rawScore;

    @Column(name = "score", nullable = false)
    @NonNull
    @Min(0)
    @Max(200)
    private Double score;

    @Column(name = "priority", nullable = false)
    @NonNull
    @Min(1)
    @Max(15)
    private Integer priority;

    @Column(name = "is_budget", nullable = false)
    @NonNull
    private Boolean isBudget;

    @Column(name = "quote_type", nullable = false)
    @NonNull
    @Enumerated(EnumType.STRING)
    private QuotaType quotaType;

    @Column(name = "is_actual", nullable = false)
    private Boolean isActual = false;

    @Column(name = "is_counted", nullable = false)
    private Boolean isCounted = false;

    @Column(name = "is_checked", nullable = false)
    private Boolean isChecked = false;
}
