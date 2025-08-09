package org.admissio.scraper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.admissio.scraper.dto.university.OsvitaUniversityDto;
import org.admissio.scraper.entity.UniversityRegion;
import org.admissio.scraper.repository.UniversityRegionRepository;
import org.admissio.scraper.utils.WebClientInsecure;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class UniversityRegionService {

    private UniversityRegionRepository repo;
    private ObjectMapper jacksonObjectMapper;
    private final WebClient webClient;
    private UniversityService universityService;
    private final String FULL_OSVITA_API_URL = "https://vstup.osvita.ua/api/";
    @Value("${scraper.osvita.api.token}")
    private String token;
    @Value("${scraper.osvita.api.cookie}")
    private String cookie;


    UniversityRegionService(UniversityRegionRepository universityRegionRepository, ObjectMapper jacksonObjectMapper, UniversityService universityService) {
        this.repo = universityRegionRepository;
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.universityService = universityService;
        this.webClient = WebClientInsecure.createInsecureWebClient(FULL_OSVITA_API_URL);
    }

    public void scrapeUniversitiesByRegion() {
        for (UniversityRegion region : repo.findAll()) {
            System.out.println("Start scrapping for universities in region: "+ region.getRegion());
            try {
                Thread.sleep(2000);
                String response = sendRequestForRawUniversity(region.getRegionCodeOsvita()).block();
                if (response != null) {
                    List<OsvitaUniversityDto> universities = jacksonObjectMapper.readValue(response,
                            jacksonObjectMapper.getTypeFactory().constructCollectionType(List.class, OsvitaUniversityDto.class));

                    if (universities != null) {
                        universities.forEach(university ->
                                universityService.processAndMapUniversity(university, region)
                        );
                    }
                }
            } catch (Exception e) {
                System.err.println("=== Error processing region = " + region.getRegion() + "===");
            }
            System.out.println(" Universities in region: "+ region.getRegion() + " are added to cache");
        }
        System.out.println("End scrapping for universities");
    }

    private Mono<String> sendRequestForRawUniversity(Integer regionCodeOsvita) {

        String body = "action=universities&vca=cloudflare&y=2025&token="+token;

        return webClient.post()
                .uri(FULL_OSVITA_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://vstup.osvita.ua/y2025/r"+regionCodeOsvita+"/174/1477258/")
                .header("Origin", "https://vstup.osvita.ua")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Priority", "u=1, i")
                .header("Host", "vstup.osvita.ua")
                .header("Accept-Language", "uk,en-US;q=0.9,en;q=0.8,ru;q=0.7")
                .header("Sec-Ch-Ua", "\"Opera\";v=\"120\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Cookie", cookie)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class);
    }

}