package org.admissio.scraper.repository;

import org.admissio.scraper.entity.Application;
import org.springframework.data.repository.CrudRepository;

public interface ApplicationRepository extends CrudRepository<Application, Long> {
}