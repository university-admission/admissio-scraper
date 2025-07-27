package org.admissio.scraper.service;

import org.admissio.scraper.entity.UniversityRegion;
import org.admissio.scraper.repository.UniversityRegionRepository;
import org.springframework.stereotype.Service;

@Service
public class UniversityRegionService {

    private UniversityRegionRepository repo;

    UniversityRegionService(UniversityRegionRepository universityRegionRepository){
        this.repo = universityRegionRepository;
    }

    public void scrapeUniversitiesByRegion(){
        for (UniversityRegion universityRegion : repo.findAll()) {

        }
    }

}
