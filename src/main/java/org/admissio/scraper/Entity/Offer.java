package org.admissio.scraper.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
    @Column(name = "faculty")
    private String faculty;

    @NonNull
    @Column(name = "education_form", nullable = false)
    private Integer educationForm;

    @NonNull
    @Column(name = "budget_places", nullable = false)
    private Integer budgetPlaces;

    @NonNull
    @Column(name = "contract_places", nullable = false)
    private Integer contractPlaces;

    @NonNull
    @Column(name = "quota1_places", nullable = false)
    private Integer quota1Places;

    @NonNull
    @Column(name = "quota2_places", nullable = false)
    private Integer quota2Places;

    @JsonIgnore
    @OneToMany(mappedBy = "offer")
    private Set<Application> applications;
}
