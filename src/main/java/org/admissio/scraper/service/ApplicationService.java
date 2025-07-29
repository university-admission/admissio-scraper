package org.admissio.scraper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.admissio.scraper.dto.application.ApplicationDto;
import org.admissio.scraper.dto.application.EdboApplicationResponseWrapper; // Змінено назву
import org.admissio.scraper.dto.application.RssEntryDto; // Імпорт RssEntryDto
import org.admissio.scraper.entity.Application;
import org.admissio.scraper.entity.Offer;
import org.admissio.scraper.entity.QuotaType; // Імпорт QuotaType
import org.admissio.scraper.entity.Student; // Імпорт Student entity
import org.admissio.scraper.repository.ApplicationRepository;
import org.admissio.scraper.repository.OfferRepository;
import org.admissio.scraper.utils.WebClientInsecure;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ApplicationService {

    private final WebClient webClient;
    private final ObjectMapper jacksonObjectMapper;
    private OfferRepository offerRepository;
    private ApplicationRepository applicationRepository;
    private StudentService studentService; // Потрібен для створення/пошуку Student
    private static final String FULL_API_URL = "https://vstup.edbo.gov.ua/offer-requests/";
    public static List<Application> applicationsCache;
    private final int BATCH_SIZE = 100;

    ApplicationService(WebClient.Builder webClientBuilder, OfferRepository offerRepository,
                       ObjectMapper jacksonObjectMapper, StudentService studentService, ApplicationRepository applicationRepository) {
        this.webClient = WebClientInsecure.createInsecureWebClient(FULL_API_URL);
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.offerRepository = offerRepository;
        this.studentService = studentService;
        this.applicationRepository = applicationRepository;
    }

    @PostConstruct
    public void init(){
        this.applicationsCache = (List<Application>) applicationRepository.findAll();
    }

    public void scrapeApplications() {
        for (Offer offer : offerRepository.findAll()) {
            int last = 0;
            boolean hasMoreApplications = true;
            try {
                while (hasMoreApplications) {
                    Thread.sleep(2000);
                    //System.out.println("Requesting applications from index: " + last);
                    String response = sendRequestForRawApplication(offer.getEdboId(), last).block();
                    if (response != null) {
                        EdboApplicationResponseWrapper wrapper = jacksonObjectMapper.readValue(response, EdboApplicationResponseWrapper.class);

                        if (wrapper != null && wrapper.getRequests() != null && !wrapper.getRequests().isEmpty()) {
                            System.out.println("Received " + wrapper.getRequests().size() + " applications.");
                            wrapper.getRequests().forEach(applicationDto ->
                                    processAndMapApplication(applicationDto, offer)
                            );
                            last += BATCH_SIZE;
                        } else {
                            //System.out.println("No more applications found for Offer ID: " + offer.getEdboId());
                            hasMoreApplications = false;
                        }
                    } else {
                        System.err.println("Received null response for Offer ID: " + offer.getEdboId() + " at index: " + last);
                        hasMoreApplications = false;
                    }
                }
                System.out.println("Finished scraping for Offer ID: " + offer.getEdboId() + ". Total applications in cache so far: " + applicationsCache.size());
            } catch (Exception e) {
                System.err.println("=== Error processing applications for offerId=" + offer.getEdboId() + " at index " + last + " ===");
            }
        }
        System.out.println("--- All offers scraped. Total applications in cache: " + applicationsCache.size() + " ---");
    }

    private Mono<String> sendRequestForRawApplication(Long offerId, int last) {
        String body = "id=" + offerId + "&last=" + last;

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
                    System.err.println("=== ERROR IN APPLICATION WEBCLIENT REQUEST for offerId=" + offerId + ", last=" + last + " ===");
                });
    }

    private void processAndMapApplication(ApplicationDto dto, Offer offer) {
        Application application = new Application();

        application.setScore(dto.getScore());

        double rawScoreSum = 0.0;
        Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)\\s*x");

        if (dto.getRssEntries() != null && dto.getRssEntries().size() >= 4) {
            for (int i = 0; i < 4; i++) {
                RssEntryDto rssEntry = dto.getRssEntries().get(i);
                if (rssEntry.getF() != null) {
                    Matcher matcher = pattern.matcher(rssEntry.getF());
                    if (matcher.find()) {
                        try {
                            rawScoreSum += Double.parseDouble(matcher.group(1));
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing raw score part: " + rssEntry.getF() + " for application " + dto.getStudentFullName());
                        }
                    }
                }
            }
        }
        application.setRawScore(rawScoreSum);


        if (dto.getPriorityAndBudgetRaw() != null && !dto.getPriorityAndBudgetRaw().isEmpty()) {
            Pattern pPattern = Pattern.compile("(\\d+)\\s*\\((\\S)\\)"); // Example: "3 (Б)"
            Matcher pMatcher = pPattern.matcher(dto.getPriorityAndBudgetRaw());
            if (pMatcher.find()) {
                try {
                    application.setPriority(Integer.parseInt(pMatcher.group(1)));
                    String budgetChar = pMatcher.group(2);
                    application.setIsBudget("Б".equalsIgnoreCase(budgetChar));
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing priority from: " + dto.getPriorityAndBudgetRaw() + " for application " + dto.getStudentFullName());
                }
            } else {
                System.err.println("Could not parse priority/budget string: " + dto.getPriorityAndBudgetRaw());
                application.setPriority(1);
                application.setIsBudget(false);
            }
        } else {
            application.setPriority(1);
            application.setIsBudget(false);
        }

        if (isApplicationAdded(dto.getStudentFullName(), rawScoreSum, application.getPriority())){
            return;
        }


        application.setQuotaType(QuotaType.GENERAL); // Default value
        if (dto.getRssEntries() != null) {
            for (RssEntryDto rssEntry : dto.getRssEntries()) {
                if ("q".equalsIgnoreCase(rssEntry.getType())) {
                    if ("Квота 1".equalsIgnoreCase(rssEntry.getQuotaName())) {
                        application.setQuotaType(QuotaType.QUOTA_1);
                        break;
                    } else if ("Квота 2".equalsIgnoreCase(rssEntry.getQuotaName())) {
                        application.setQuotaType(QuotaType.QUOTA_2);
                        break;
                    }
                }
            }
        }

        Student student = studentService.getOrCreateStudent(dto.getStudentFullName(), rawScoreSum);
        application.setStudent(student);

        application.setOffer(offer);


        applicationsCache.add(application);
    }

    private boolean isApplicationAdded(String fullName, Double rawScore, Integer priority) {
        for (Application application : applicationsCache) {
            if (application.getRawScore().equals(rawScore) && application.getPriority().equals(priority)
            && application.getStudent().getFullName().equalsIgnoreCase(fullName)) {
                return true;
            }
        }
        return false;
    }
}