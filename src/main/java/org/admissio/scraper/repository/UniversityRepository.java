package org.admissio.scraper.repository;

import org.admissio.scraper.entity.University;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UniversityRepository extends CrudRepository<University, Long> {

    @Query("SELECT u FROM University u WHERE u.universityCode = :universityCode")
    Optional<University> findByUniversityCode(@Param("universityCode") Integer universityCode);

}
