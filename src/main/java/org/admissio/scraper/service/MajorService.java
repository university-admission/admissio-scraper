package org.admissio.scraper.service;

import org.admissio.scraper.entity.Major;
import org.admissio.scraper.repository.MajorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MajorService {
    @Autowired
    private MajorRepository majorRepository;

    public Optional<Major> findMajorByCode(String majorCode) {
        return majorRepository.findByMajorCodeIgnoreCase(majorCode);
    }
}
