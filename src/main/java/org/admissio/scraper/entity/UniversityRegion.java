package org.admissio.scraper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "university_regions")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
public class UniversityRegion {
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @NonNull
    @Column(name = "region", nullable = false)
    private String region;

    @NonNull
    @Column(name = "region_code", nullable = false)
    private Integer regionCode;

    @JsonIgnore
    @OneToMany(mappedBy = "universityRegion")
    private Set<University> universities;
}
