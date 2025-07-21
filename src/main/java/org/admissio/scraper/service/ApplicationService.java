package org.admissio.scraper.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ApplicationService {

    private final WebClient webClient;
    private static final String FULL_API_URL = "https://vstup2024.edbo.gov.ua/offer-requests/";

    ApplicationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }


    public String postApplicationRequest(String id, int last) {
        RestTemplate restTemplate = new RestTemplate();

        // Headers for post request
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.set("X-Requested-With", "XMLHttpRequest");
        headers.set("Referer", "https://vstup2024.edbo.gov.ua/");

        // Create a body of request
        String body = "id=" + id + "&last=" + last;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        // Logs
        System.out.println("=== ЗАПИТ ДО EDBO ===");
        System.out.println("POST https://vstup2024.edbo.gov.ua/offer-requests/");
        headers.forEach((k, v) -> System.out.println(k + ": " + v));
        System.out.println("Тіло: " + body);
        System.out.println("======================");

        // Send a request
        ResponseEntity<String> response = restTemplate.exchange(
                "https://vstup2024.edbo.gov.ua/offer-requests/",
                HttpMethod.POST,
                request,
                String.class
        );

        // Logs
        System.out.println("=== ВІДПОВІДЬ ВІД EDBO ===");
        System.out.println("Статус: " + response.getStatusCode());
        response.getHeaders().forEach((k, v) -> System.out.println(k + ": " + v));
        System.out.println("Тіло: " + response.getBody());
        System.out.println("==========================");

        return response.getBody();
    }

}
