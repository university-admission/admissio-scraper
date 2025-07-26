package org.admissio.scraper.service;

import org.admissio.scraper.dto.university.EdboUniversityResponseWrapper;
import org.admissio.scraper.dto.university.UniversityDto;
import org.admissio.scraper.entity.Offer;
import org.admissio.scraper.entity.University;
import org.admissio.scraper.repository.UniversityRepository;
import org.admissio.scraper.utils.WebClientInsecure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UniversityService {
    private final OfferService offerService;
    private UniversityRepository universityRepository;
    private ObjectMapper jacksonObjectMapper;
    private static final String FULL_API_URL = "https://vstup.edbo.gov.ua/offers-universities/";
    private final WebClient webClient;

    UniversityService(UniversityRepository universityRepository, ObjectMapper jacksonObjectMapper, OfferService offerService){
        this.webClient = WebClientInsecure.createInsecureWebClient(FULL_API_URL);
        this.universityRepository = universityRepository;
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.offerService = offerService;
    }

    public void scrapeUniversities() {
        Integer[] universityIds = {79}; // Тестовий список, заміниш на реальні

        Optional<University> uni = universityRepository.findByUniversityCode(79);

        List<University> universities = new ArrayList<>();
        universities.add(uni.get());

        for (University university : universities) {
            Integer universityId = university.getUniversityCode();
            sendRequestForRawUniversity(universityId).subscribe(
                    rawResponse -> {
                        try {
                            EdboUniversityResponseWrapper wrapper = jacksonObjectMapper.readValue(rawResponse, EdboUniversityResponseWrapper.class);

                            if (wrapper.getUniversities() != null && !wrapper.getUniversities().isEmpty()) {
                                //wrapper.getUniversities().forEach(this::processAndMapUniversity);
                                wrapper.getUniversities().forEach(universityDto -> {
                                   int offerIds[] = getOfferIds(universityDto.getIds());
                                    offerService.scrapeOffers(offerIds, university);
                                });
                            } else {
                                System.out.println("Empty university response for ID: " + universityId);
                            }

                        } catch (Exception e) {
                            System.err.println("Failed to parse university JSON for ID " + universityId + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    },
                    error -> System.err.println("Error during university request: " + error.getMessage())
            );
        }
    }

    private Mono<String> sendRequestForRawUniversity(Integer universityId) {
        String body = "qualification=1&education_base=40&university=" + universityId;

        return webClient.post()
                .uri(FULL_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://vstup.edbo.gov.ua")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> {
                    System.err.println("=== ERROR IN UNIVERSITY WEBCLIENT REQUEST ===");
                    e.printStackTrace();
                });
    }

    private void processAndMapUniversity(UniversityDto universityDto) {
        try {
            if (universityDto == null) return;

            University uni = new University();
            uni.setUniversityCode(universityDto.getUid());
            uni.setUniversityName(universityDto.getUn());

            universityRepository.save(uni);
            System.out.println("Saved university: " + universityDto.getUn());

        } catch (Exception e) {
            System.err.println("Error mapping UniversityDto: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private int[] getOfferIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return new int[0];
        }

        String[] parts = ids.split(",");
        int[] result = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }

        return result;
    }

    public Optional<University> findByUniversityCode(Integer universityCode) {
        return universityRepository.findByUniversityCode(universityCode);
    }

}
