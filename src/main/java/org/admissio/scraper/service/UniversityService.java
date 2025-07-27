package org.admissio.scraper.service;

import org.admissio.scraper.dto.university.UniversityDto;
import org.admissio.scraper.entity.University;
import org.admissio.scraper.entity.UniversityRegion;
import org.admissio.scraper.repository.UniversityRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UniversityService {
    private OfferService offerService;
    private UniversityRepository universityRepository;

    UniversityService(UniversityRepository universityRepository, OfferService offerService) {
        this.universityRepository = universityRepository;
        this.offerService = offerService;
    }

    public void processAndMapUniversity(UniversityDto universityDto, UniversityRegion universityRegion) {
        try {

            Optional<University> universityOptional = universityRepository.findByUniversityCode(universityDto.getUid());

            if (universityOptional.isPresent()) {
                offerService.scrapeOffers(universityDto.getIds(), universityOptional.get());
            } else {

                University uni = new University();

                uni.setUniversityCode(universityDto.getUid());
                uni.setUniversityName(universityDto.getUn());
                uni.setUniversityRegion(universityRegion);
                universityRepository.save(uni);

                offerService.scrapeOffers(universityDto.getIds(), uni);
            }


        } catch (Exception e) {
            System.err.println("Error mapping UniversityDto: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
