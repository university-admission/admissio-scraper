package org.admissio.scraper.repository;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.NonNull;
import org.admissio.scraper.entity.Application;
import org.admissio.scraper.entity.Offer;
import org.admissio.scraper.entity.Student;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ApplicationRepository extends CrudRepository<Application, Long> {
    List<Application> findAllByOfferOrderByScoreDesc(@NonNull Offer offer);

    List<Application> findAllByStudentAndRawScoreOrderByPriority(@NonNull Student student, @NonNull @Min(0) @Max(200) Double rawScore);
}
