package org.admissio.scraper.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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

    @Column(name = "score", nullable = false)
    @NonNull
    private Float score;

    @Column(name = "priority", nullable = false)
    @NonNull
    private Integer priority;

    @Column(name = "quota", nullable = false)
    @NonNull
    private Integer quota;

    @Column(name = "is_budget", nullable = false)
    @NonNull
    private Boolean isBudget;

    @Column(name = "is_actual", nullable = false)
    private Boolean isActual = false;

    @Column(name = "is_counted", nullable = false)
    private Boolean isCounted = false;
}
