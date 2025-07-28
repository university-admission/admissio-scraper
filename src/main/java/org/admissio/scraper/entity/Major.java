package org.admissio.scraper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "majors")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
public class Major {

    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @NonNull
    @Column(name = "major_name", nullable = false)
    private String majorName;

    @NonNull
    @Column(name = "major_code", nullable = false, unique = true)
    private String majorCode;

    @NonNull
    @Column(name = "uk_lang_coef", nullable = false)
    @Min(0)
    @Max(1)
    private Double ukLanguageCoef;

    @NonNull
    @Column(name = "math_coef", nullable = false)
    @Min(0)
    @Max(1)
    private Double mathCoef;

    @NonNull
    @Column(name = "history_coef", nullable = false)
    @Min(0)
    @Max(1)
    private Double historyCoef;

    @NonNull
    @Column(name = "uk_literature_coef", nullable = false)
    @Min(0)
    @Max(1)
    private Double ukLiteratureCoef;

    @NonNull
    @Column(name = "foreign_lang_coef", nullable = false)
    @Min(0)
    @Max(1)
    private Double foreignLangCoef;

    @NonNull
    @Column(name = "biology_coef", nullable = false)
    @Min(0)
    @Max(1)
    private Double biologyCoef;

    @NonNull
    @Column(name = "geography_coef", nullable = false)
    @Min(0)
    @Max(1)
    private Double geographyCoef;

    @NonNull
    @Column(name = "physics_coef", nullable = false)
    @Min(0)
    @Max(1)
    private Double physicsCoef;

    @NonNull
    @Column(name = "chemistry_coef", nullable = false)
    @Min(0)
    @Max(1)
    private Double chemistryCoef;

    @NonNull
    @Column(name = "competition_coef", nullable = false)
    @Min(0)
    @Max(1)
    private Double competitionCoef;

    @NonNull
    @Column(name = "major_coef", nullable = false)
    @Max(2)
    @Min(1)
    private Double majorCoef;

    @JsonIgnore
    @OneToMany(mappedBy = "major")
    private Set<Offer> offers;
}
