package org.admissio.scraper.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.admissio.scraper.entity.*;
import org.admissio.scraper.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchSavingService {
    @NonNull
    OfferRepository offerRepository;
    @NonNull
    MajorRepository majorRepository;
    @NonNull
    UniversityRepository universityRepository;
    @NonNull
    StudentRepository studentRepository;
    @NonNull
    ApplicationRepository applicationRepository;
    @NonNull
    EntityManager entityManager;
    private final int batchSize = 100;

    @Transactional
    public void saveOffersInBatch(List<Offer> offers) {
        for (int i = 0; i < offers.size(); i += batchSize) {
            int end = Math.min(i + batchSize, offers.size());
            List<Offer> sub = offers.subList(i, end);
            offerRepository.saveAll(sub);
            entityManager.flush();
            entityManager.clear();
        }
    }

    @Transactional
    public void saveMajorsInBatch(List<Major> majors) {
        for (int i = 0; i < majors.size(); i += batchSize) {
            int end = Math.min(i + batchSize, majors.size());
            List<Major> sub = majors.subList(i, end);
            majorRepository.saveAll(sub);
            entityManager.flush();
            entityManager.clear();
        }
    }

    @Transactional
    public void saveUniversitiesInBatch(List<University> universities) {
        for (int i = 0; i < universities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, universities.size());
            List<University> sub = universities.subList(i, end);
            universityRepository.saveAll(sub);
            entityManager.flush();
            entityManager.clear();
        }
    }

    @Transactional
    public void saveStudentsInBatch(List<Student> students) {
        for (int i = 0; i < students.size(); i += batchSize) {
            int end = Math.min(i + batchSize, students.size());
            List<Student> sub = students.subList(i, end);
            studentRepository.saveAll(sub);
            entityManager.flush();
            entityManager.clear();
        }
    }

    @Transactional
    public void saveApplicationsInBatch(List<Application> applications) {
        for (int i = 0; i < applications.size(); i += batchSize) {
            int end = Math.min(i + batchSize, applications.size());
            List<Application> sub = applications.subList(i, end);
            applicationRepository.saveAll(sub);
            entityManager.flush();
            entityManager.clear();
        }
    }

}
