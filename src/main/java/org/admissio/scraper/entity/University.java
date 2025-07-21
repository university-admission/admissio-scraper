package org.admissio.scraper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "universities")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
public class University {

    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @NonNull
    @Column(name = "university_name", nullable = false)
    private String universityName;

    @NonNull
    @Column(name = "university_code", nullable = false, unique = true)
    private Integer universityCode;

    @JsonIgnore
    @ManyToOne
    @NonNull
    @JoinColumn(name = "university_region_id", referencedColumnName = "id", nullable = false)
    private UniversityRegion universityRegion;

    @JsonIgnore
    @OneToMany(mappedBy = "university")
    private Set<Offer> offers;
}
