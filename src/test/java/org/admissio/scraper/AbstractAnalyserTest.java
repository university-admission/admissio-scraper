package org.admissio.scraper;

import org.admissio.scraper.entity.*;
import org.admissio.scraper.repository.*;
import org.admissio.scraper.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class AbstractAnalyserTest {
    @Autowired
    protected UniversityRegionRepository regionRepo;
    @Autowired protected UniversityRepository universityRepo;
    @Autowired protected MajorRepository majorRepo;
    @Autowired protected OfferRepository offerRepo;
    @Autowired protected StudentRepository studentRepo;
    @Autowired protected ApplicationRepository applicationRepo;
    @Autowired protected AnalyserService analyserService;

    protected UniversityRegion defaultRegion;
    protected University defaultUniversity;
    protected Major defaultMajor;

    @BeforeEach
    void setUpBase() {
        applicationRepo.deleteAll();
        studentRepo.deleteAll();
        offerRepo.deleteAll();

        majorRepo.deleteAll();
        universityRepo.deleteAll();
        regionRepo.deleteAll();

        defaultRegion = regionRepo.save(new UniversityRegion("Kyiv", 80));
        defaultUniversity = universityRepo.save(new University("NaUKMA", 1234, defaultRegion));
        defaultMajor = majorRepo.save(new Major("CS", "121", 0.2, 0.4, 0.1, 0.1, 0.2, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0));
    }

    protected Offer createOffer(String name, int budgetPlaces, int contractPlaces, int quota1Places, int quota2Places) {
        return offerRepo.save(new Offer(
                1L, name, defaultMajor, defaultUniversity, "FI", "Program", 25000, EducationForm.FULL_TIME,
                budgetPlaces, contractPlaces, quota1Places, quota2Places,
                100, 100, 100, 100, 100, 100, 100, 100, 100,
                100, 100, 0, 1.0
        ));
    }

    protected Student createStudent(String name, double score) {
        return studentRepo.save(new Student(name, score));
    }

    protected void createApplication(Student student, Offer offer, int priority, boolean isBudget, QuotaType quotaType) {
        applicationRepo.save(new Application(student, offer, student.getRawScore(), priority, isBudget, quotaType));
    }
}
