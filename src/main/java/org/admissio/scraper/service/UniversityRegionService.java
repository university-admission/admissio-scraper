package org.admissio.scraper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.admissio.scraper.dto.offer.EdboResponseWrapper;
import org.admissio.scraper.dto.university.EdboUniversityResponseWrapper;
import org.admissio.scraper.entity.UniversityRegion;
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
    private BatchSavingService  batchSavingService;
    private UniversityService universityService;
    private static final String FULL_API_URL = "https://vstup.edbo.gov.ua/offers-universities/";


    UniversityRegionService(UniversityRegionRepository universityRegionRepository, ObjectMapper jacksonObjectMapper, UniversityService universityService,
    BatchSavingService batchSavingService) {
        this.repo = universityRegionRepository;
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.universityService = universityService;
        this.webClient = WebClientInsecure.createInsecureWebClient(FULL_API_URL);
        this.batchSavingService = batchSavingService;
    }

    public void scrapeUniversitiesByRegion(String[] majorCodes) {
        for (UniversityRegion region : repo.findAll()) {
            for (String majorCode : majorCodes) {
                try {
                    Thread.sleep(2000);
                    String response = sendRequestForRawUniversity(majorCode, region.getRegionCode()).block();
                    if (response != null) {
                        EdboUniversityResponseWrapper wrapper = jacksonObjectMapper.readValue(response, EdboUniversityResponseWrapper.class);
                        if (wrapper != null && wrapper.getUniversities() != null) {
                            wrapper.getUniversities().forEach(university ->
                                    universityService.processAndMapUniversity(university, region)
                            );
                        }
                    }
                } catch (Exception e) {
                    System.err.println("=== Error processing majorCode=" + majorCode + ", regionCode=" + region.getRegionCode() + " ===");
                }
            }
        }
    }

    private Mono<String> sendRequestForRawUniversity(String majorCode, Integer regionCode) {
        String body = "qualification=1&education_base=40&speciality=" + majorCode + "&region=" + regionCode;

        return webClient.post()
                .uri(FULL_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://vstup.edbo.gov.ua")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> {
                    System.err.println("=== ERROR IN UNIVERSITY WEBCLIENT REQUEST ===");
                });
    }

}
