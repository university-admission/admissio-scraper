package org.admissio.scraper.service;

import org.admissio.scraper.entity.University;
import org.admissio.scraper.repository.UniversityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UniversityService {
    @Autowired
    private UniversityRepository universityRepository;

    public Optional<University> findByUniversityCode(Integer universityCode) {
        return universityRepository.findByUniversityCode(universityCode);
    }

}
