package org.admissio.scraper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "offers")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
public class Offer {

    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @NonNull
    @Column(name = "edbo_id", nullable = false)
    private Long edboId;

    @NonNull
    @Column(name = "name", nullable = false)
    private String name;

    @JsonIgnore
    @NonNull
    @ManyToOne
    @JoinColumn(name = "major_id", referencedColumnName = "id", nullable = false)
    private Major major;

    @JsonIgnore
    @NonNull
    @ManyToOne
    @JoinColumn(name = "university_id", referencedColumnName = "id", nullable = false)
    private University university;

    @NonNull
    @Column(name = "faculty", nullable = false)
    private String faculty;

    @NonNull
    @Column(name = "educational_program", nullable = false)
    private String educationalProgram;

    @NonNull
    @Column(name = "price", nullable = false)
    private Integer price;

    @NonNull
    @Column(name = "education_form", nullable = false)
    private String educationForm;

    @NonNull
    @Column(name = "budget_places", nullable = false)
    private Integer budgetPlaces;

    @Column(name = "budget_places_count", nullable = false)
    private Integer budgetPlacesCount = 0;

    @Min(0)
    @Max(200)
    @Column(name = "min_budget_score", nullable = false)
    private Double minBudgetScore = 0d;

    @NonNull
    @Column(name = "contract_places", nullable = false)
    private Integer contractPlaces;

    @Column(name = "contract_places_count", nullable = false)
    private Integer contractPlacesCount = 0;

    @Min(0)
    @Max(200)
    @Column(name = "min_contract_score", nullable = false)
    private Double minContractScore = 0d;

    @NonNull
    @Column(name = "quota1_places", nullable = false)
    private Integer quota1Places;

    @Column(name = "quota1_places_count", nullable = false)
    private Integer quota1PlacesCount = 0;

    @Min(0)
    @Max(200)
    @Column(name = "min_quota1_score", nullable = false)
    private Double minQuota1Score = 0d;

    @NonNull
    @Column(name = "quota2_places", nullable = false)
    private Integer quota2Places;

    @Column(name = "quota2_places_count", nullable = false)
    private Integer quota2PlacesCount = 0;

    @Min(0)
    @Max(200)
    @Column(name = "min_quota2_score", nullable = false)
    private Double minQuota2Score = 0d;

    @NonNull
    @Column(name = "min_uk_lang_score", nullable = false)
    @Min(100)
    @Max(200)
    private Integer minUkLangScore;

    @NonNull
    @Column(name = "min_math_score", nullable = false)
    @Min(100)
    @Max(200)
    private Integer minMathScore;

    @NonNull
    @Column(name = "min_history_score", nullable = false)
    @Min(100)
    @Max(200)
    private Integer minHistoryScore;

    @NonNull
    @Column(name = "min_uk_lit_score", nullable = false)
    @Min(100)
    @Max(200)
    private Integer minUkLitScore;

    @NonNull
    @Column(name = "min_foreign_lang_score",  nullable = false)
    @Min(100)
    @Max(200)
    private Integer minForeignLangScore;

    @NonNull
    @Column(name = "min_biology_score",  nullable = false)
    @Min(100)
    @Max(200)
    private Integer minBiologyScore;

    @NonNull
    @Column(name = "min_geography_score", nullable = false)
    @Min(100)
    @Max(200)
    private Integer minGeographyScore;

    @NonNull
    @Column(name = "min_physics_score", nullable = false)
    @Min(100)
    @Max(200)
    private Integer minPhysicsScore;

    @NonNull
    @Column(name = "min_chemistry_score", nullable = false)
    @Min(100)
    @Max(200)
    private Integer minChemistryScore;

    @NonNull
    @Column(name = "min_competition_score",  nullable = false)
    @Min(100)
    @Max(200)
    private Integer minCompetitionScore;

    @NonNull
    @Column(name = "min_application_score",  nullable = false)
    @Min(100)
    @Max(200)
    private Integer minApplicationScore;

    @NonNull
    @Column(name = "additional_points", nullable = false)
    @Min(0)
    private Integer additionalPoints;

    @NonNull
    @Column(name = "region_coef", nullable = false)
    @Min(1)
    @Max(2)
    private Double regionCoef;

    @JsonIgnore
    @OneToMany(mappedBy = "offer")
    private Set<Application> applications;
}
