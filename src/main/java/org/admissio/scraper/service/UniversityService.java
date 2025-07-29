package org.admissio.scraper.service;

import jakarta.annotation.PostConstruct;
import org.admissio.scraper.dto.university.UniversityDto;
import org.admissio.scraper.entity.University;
import org.admissio.scraper.entity.UniversityRegion;
import org.admissio.scraper.repository.UniversityRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UniversityService {
    private OfferService offerService;
    private UniversityRepository universityRepository;
    public static List<University> universitiesCache;

    UniversityService(UniversityRepository universityRepository, OfferService offerService) {
        this.universityRepository = universityRepository;
        this.offerService = offerService;
    }

    @PostConstruct
    public void init() {
        this.universitiesCache = (List<University>) universityRepository.findAll();
    }

    public void processAndMapUniversity(UniversityDto universityDto, UniversityRegion universityRegion) {
        try {

            Optional<University> universityOptional = findUniversityByUniversityCode(universityDto.getUid());

            if (universityOptional.isPresent()) {
                offerService.scrapeOffers(universityDto.getIds(), universityOptional.get());
            } else {

                University uni = new University();

                uni.setUniversityCode(universityDto.getUid());
                uni.setUniversityName(universityDto.getUn());
                uni.setUniversityRegion(universityRegion);
                //universityRepository.save(uni);
                universitiesCache.add(uni);

                offerService.scrapeOffers(universityDto.getIds(), uni);
            }


        } catch (Exception e) {
            System.err.println("Error mapping UniversityDto: " + e.getMessage());
        }

    }

    private Optional<University> findUniversityByUniversityCode(Integer universityCode) {
        for (University university : universitiesCache) {
            if (university.getUniversityCode().equals(universityCode)) {
                return Optional.of(university);
            }
        }
        return Optional.empty();
    }

}
