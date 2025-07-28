package org.admissio.scraper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.admissio.scraper.dto.university.EdboUniversityResponseWrapper;
import org.admissio.scraper.repository.UniversityRegionRepository;
import org.admissio.scraper.utils.WebClientInsecure;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.List;

@Service
public class UniversityRegionService {

    private UniversityRegionRepository repo;
    private ObjectMapper jacksonObjectMapper;
    private final WebClient webClient;
    private UniversityService universityService;
    private static final String FULL_API_URL = "https://vstup.edbo.gov.ua/offers-universities/";
    private final String[] informaticsMajors = {"F1", "F2", "F3", "F4", "F5", "F6", "F7"};
    private final String[] testMajors = {"B10"};
    private final String[] majorCodes = {
            "A1", "A2", "A3", "A4", "A5", "A6", "A7", "B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9",
            "B10", "B11", "B12", "B13", "B14", "C1", "C2", "C3", "C4", "C5", "C6", "C7", "D1", "D2",
            "D3", "D4", "D5", "D7", "D8", "D9", "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8",
            "F1", "F2", "F3", "F4", "F5", "F6", "F7", "G1", "G2", "G3", "G4", "G5", "G6", "G7",
            "G8", "G9", "G10", "G11", "G12", "G13", "G14", "G15", "G16", "G17", "G18", "G19",
            "G20", "G21", "G22", "H1", "H2", "H3", "H4", "H5", "H6", "H7", "I1", "I2", "I3",
            "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I11", "J1", "J2", "J3", "J4", "J5", "J6",
            "J7", "J8", "K1", "K2", "K3", "K4", "K5", "K6", "k7", "K8", "K9", "K10"
    };

    UniversityRegionService(UniversityRegionRepository universityRegionRepository, ObjectMapper jacksonObjectMapper, UniversityService universityService) {
        this.repo = universityRegionRepository;
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.universityService = universityService;
        this.webClient = WebClientInsecure.createInsecureWebClient(FULL_API_URL);
    }

    public Mono<Void> scrapeUniversitiesByRegion() {
        return Flux.fromIterable(repo.findAll())
                .concatMap(universityRegion -> {
                    //System.out.println("Processing region: " + universityRegion.getRegion() + " (" + universityRegion.getRegionCode() + ")");
                    List<Mono<String>> regionRequests = new ArrayList<>();
                    for (String majorCode : majorCodes) {
                        regionRequests.add(sendRequestForRawUniversity(majorCode, universityRegion.getRegionCode())
                                .doOnError(e -> System.err.println("Error for majorCode " + majorCode + " in region " + universityRegion.getRegionCode() + ": " + e.getMessage()))
                        );
                    }

                    return Flux.merge(regionRequests)
                            .flatMap(rawResponse -> {
                                try {
                                    EdboUniversityResponseWrapper wrapper = jacksonObjectMapper.readValue(rawResponse, EdboUniversityResponseWrapper.class);
                                    if (wrapper.getUniversities() != null && !wrapper.getUniversities().isEmpty()) {
                                        wrapper.getUniversities().forEach(universityDto -> universityService.processAndMapUniversity(universityDto, universityRegion));
                                        return Mono.empty();
                                    } else {
                                        System.out.println("Empty university response for region: " + universityRegion.getRegionCode());
                                        return Mono.empty();
                                    }
                                } catch (Exception e) {
                                    System.err.println("Failed to parse university JSON for region: " + universityRegion.getRegionCode() + ": " + e.getMessage());
                                    e.printStackTrace();
                                    return Mono.error(e);
                                }
                            })
                            .then(Mono.fromRunnable(() -> {
                                System.out.println("All data for region " + universityRegion.getRegion() + " processed. Initiating batch save for this region...");
                                System.out.println("Batch save for region " + universityRegion.getRegion() + " completed.");
                            }));
                })
                .then();
    }

    private Mono<String> sendRequestForRawUniversity(String majorCode, Integer regionCode) {
        String body = "qualification=1&education_base=40&speciality=" + majorCode + "&region=" + regionCode;

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

}
