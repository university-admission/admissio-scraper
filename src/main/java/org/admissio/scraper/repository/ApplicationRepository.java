package org.admissio.scraper.repository;

import org.admissio.scraper.entity.Application;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ApplicationRepository extends CrudRepository<Application, Long> {
    @Query("SELECT a FROM Application a JOIN FETCH a.offer JOIN FETCH a.student ORDER BY a.score DESC")
    List<Application> findAllWithAssociations();
}