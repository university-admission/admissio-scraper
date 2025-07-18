package org.admissio.scraper.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
    @Column(name = "major_code", nullable = false)
    private String majorCode;

    @Column(name = "major_code_old")
    private Integer majorCodeOld;

    @NonNull
    @Column(name = "uk_lang_coef", nullable = false)
    private Double ukLanguageCoef;

    @NonNull
    @Column(name = "math_coef", nullable = false)
    private Double mathCoef;

    @NonNull
    @Column(name = "history_coef", nullable = false)
    private Double historyCoef;

    @NonNull
    @Column(name = "uk_literature_coef", nullable = false)
    private Double ukLiteratureCoef;

    @NonNull
    @Column(name = "foreign_lang_coef", nullable = false)
    private Double foreignLangCoef;

    @NonNull
    @Column(name = "biology_coef", nullable = false)
    private Double bioligyCoef;

    @NonNull
    @Column(name = "geography_coef", nullable = false)
    private Double geographyCoef;

    @NonNull
    @Column(name = "Physics_coef", nullable = false)
    private Double physicsCoef;

    @NonNull
    @Column(name = "chemistry_coef", nullable = false)
    private Double chemistryCoef;

    @JsonIgnore
    @OneToMany(mappedBy = "major")
    private Set<Offer> offers;
}
