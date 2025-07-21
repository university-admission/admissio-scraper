package org.admissio.scraper.repository;

import org.admissio.scraper.entity.Student;
import org.springframework.data.repository.CrudRepository;

public interface StudentRepository extends CrudRepository<Student, Long> {
}
