package org.admissio.scraper.service;

import jakarta.annotation.PostConstruct;
import org.admissio.scraper.dto.university.OsvitaUniversityDto;
import org.admissio.scraper.entity.University;
import org.admissio.scraper.entity.UniversityRegion;
import org.admissio.scraper.repository.UniversityRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UniversityService {
    private UniversityRepository universityRepository;
    private final static Map<Integer, University> universitiesCache = new HashMap<>();

    UniversityService(UniversityRepository universityRepository) {
        this.universityRepository = universityRepository;
    }

    @PostConstruct
    public void init() {
        List<University> universities = (List<University>) universityRepository.findAll();
        for (University university : universities) {
            universitiesCache.put(university.getUniversityCode(), university);
        }
    }

    public void processAndMapUniversity(OsvitaUniversityDto uniDto, UniversityRegion universityRegion) {

        if (!universitiesCache.containsKey(uniDto.getUniversityId())) {
            University uni = new University();

            uni.setUniversityCode(uniDto.getUniversityId());
            uni.setUniversityName(uniDto.getUniversityFullName());
            uni.setUniversityRegion(universityRegion);
            universitiesCache.put(uni.getUniversityCode(), uni);
        }

    }

    public static University getUniversityByCode(Integer code) {
        return universitiesCache.get(code);
    }

    public static List<University> getUniversitiesCacheList() {
        return new ArrayList<>(universitiesCache.values());
    }

}
