package org.admissio.scraper.repository;

import org.admissio.scraper.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MajorRepository extends JpaRepository<Major, Long> {

    Optional<Major> findByMajorCodeIgnoreCase(String majorCode);

}
