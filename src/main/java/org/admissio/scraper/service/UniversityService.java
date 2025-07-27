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
    private OfferService offerService;
    private UniversityRepository universityRepository;
    private ObjectMapper jacksonObjectMapper;
    private static final String FULL_API_URL = "https://vstup.edbo.gov.ua/offers-universities/";
    private final WebClient webClient;
    private final String[] majorCodes = {
            "A2", "A3", "A5", "A6", "A7", "B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9",
            "B10", "B11", "B12", "B13", "C1", "C2", "C3", "C4", "C5", "C6", "C7", "D1", "D2",
            "D3", "D4", "D5", "D7", "D8", "D9", "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8",
            "F1", "F2", "F3", "F4", "F5", "F6", "F7", "G1", "G2", "G3", "G4", "G5", "G6", "G7",
            "G8", "G9", "G10", "G11", "G12", "G13", "G14", "G15", "G16", "G17", "G18", "G19",
            "G20", "G21", "G22", "H1", "H2", "H3", "H4", "H5", "H6", "H7", "I1", "I2", "I3",
            "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I11", "J2", "J3", "J4", "J5", "J6",
            "J7", "J8", "K3", "K4", "K8", "K9", "K10"
    };
    private final String[] informaticsMajors = {"F1", "F2", "F3", "F4", "F5", "F6", "F7"};
    private final List<University> universities = new ArrayList<>();

    UniversityService(UniversityRepository universityRepository, ObjectMapper jacksonObjectMapper, OfferService offerService) {
        this.webClient = WebClientInsecure.createInsecureWebClient(FULL_API_URL);
        this.universityRepository = universityRepository;
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.offerService = offerService;
    }

    public void scrapeUniversities() {

        for (University university : universityRepository.findAll()) {
            Integer universityId = university.getUniversityCode();
            for (String majorCode : informaticsMajors) {
                sendRequestForRawUniversity(universityId, majorCode).subscribe(
                        rawResponse -> {
                            try {
                                EdboUniversityResponseWrapper wrapper = jacksonObjectMapper.readValue(rawResponse, EdboUniversityResponseWrapper.class);

                                if (wrapper.getUniversities() != null && !wrapper.getUniversities().isEmpty()) {
                                    //wrapper.getUniversities().forEach(this::processAndMapUniversity);
                                    wrapper.getUniversities().forEach(universityDto -> {
                                        offerService.scrapeOffers(universityDto.getIds(), university);
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
    }

    private Mono<String> sendRequestForRawUniversity(Integer universityId, String majorCode) {
        String body = "qualification=1&education_base=40&speciality=" + majorCode + "&university=" + universityId;

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

    public Optional<University> findByUniversityCode(Integer universityCode) {
        return universityRepository.findByUniversityCode(universityCode);
    }

}
