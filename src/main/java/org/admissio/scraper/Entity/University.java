package org.admissio.scraper.Entity;

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
    @Column(name = "university_code", nullable = false)
    private String universityCode;

    @NonNull
    @Column(name = "university_region", nullable = false)
    private String universityRegion;

    @JsonIgnore
    @OneToMany(mappedBy = "university")
    private Set<Offer> offers;
}
