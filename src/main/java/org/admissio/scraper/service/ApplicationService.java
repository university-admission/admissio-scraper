package org.admissio.scraper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.admissio.scraper.dto.application.ApplicationDto;
import org.admissio.scraper.dto.application.EdboApplicationResponseWrapper;
import org.admissio.scraper.dto.application.RssEntryDto;
import org.admissio.scraper.entity.Application;
import org.admissio.scraper.entity.Offer;
import org.admissio.scraper.entity.QuotaType;
import org.admissio.scraper.entity.Student;
import org.admissio.scraper.repository.ApplicationRepository;
import org.admissio.scraper.repository.OfferRepository;
import org.admissio.scraper.utils.VstupDataDecryptor;
import org.admissio.scraper.utils.WebClientInsecure;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ApplicationService {

    private final WebClient webClient;
    private final ObjectMapper jacksonObjectMapper;
    private OfferRepository offerRepository;
    private ApplicationRepository applicationRepository;
    private StudentService studentService;
    private BatchSavingService batchSavingService;
    private static final String FULL_API_URL = "https://vstup.edbo.gov.ua/offer-requests/";
    @Value("${scraper.edbo.api.cookie}")
    private String cookie;
    public static List<Application> applicationsCache = new ArrayList<>();
    public static Set<String> applicationKeys = new HashSet<>();
    private final int BATCH_SIZE = 100;
    private int applicationsSaved = 0;

    ApplicationService(OfferRepository offerRepository, ObjectMapper jacksonObjectMapper, StudentService studentService,
                       ApplicationRepository applicationRepository, BatchSavingService batchSavingService) {
        this.webClient = WebClientInsecure.createInsecureWebClient(FULL_API_URL);
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.offerRepository = offerRepository;
        this.studentService = studentService;
        this.applicationRepository = applicationRepository;
        this.batchSavingService = batchSavingService;
    }

    @PostConstruct
    public void init(){
        for (Application app : applicationRepository.findAll()) {
            applicationKeys.add(generateApplicationKey(app.getStudent().getFullName(), app.getRawScore(), app.getPriority()));
        }
    }

    public void scrapeApplications() {
        System.out.println("Scraping applications");
        long startOfferId = -1;
        boolean startScraping = startOfferId <= 0;
        for (Offer offer : offerRepository.findAll()) {

            if (!startScraping && offer.getEdboId().equals(startOfferId)) {
                startScraping = true; // Start point for scrapping
            }

            if (!startScraping) {
                //System.out.println("Skipping Offer ID: " + offer.getEdboId());
                continue;
            }

            int last = 0;
            boolean hasMoreApplications = true;
            try {
                while (hasMoreApplications) {
                    if (last > 500){
                        break;
                    }
                    Thread.sleep(2000);
                    System.out.println("Requesting applications from index: " + last);
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
                            System.out.println("No more applications found for Offer ID: " + offer.getEdboId());
                            hasMoreApplications = false;
                        }
                    } else {
                        System.err.println("Received null response for Offer ID: " + offer.getEdboId() + " at index: " + last);
                        hasMoreApplications = false;
                    }
                }
                System.out.println("Finished scraping for Offer ID: " + offer.getEdboId() + ". Total applications saved so far: " + applicationsSaved);
            } catch (Exception e) {
                System.err.println("=== Error processing applications for offerId=" + offer.getEdboId() + " at index " + last + " ===");
            }
            System.out.println("--- OfferId" + offer.getEdboId() + " scraped. Total applications in cache: " + applicationsCache.size() + " ---");
            batchSavingService.saveApplicationsInBatch(applicationsCache);
            applicationsCache.clear();
        }
    }

    private Mono<String> sendRequestForRawApplication(Long offerId, int last) {
        String body = "id=" + offerId + "&last=" + last;

        return webClient.post()
                .uri(FULL_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Accept-Language", "uk,en-US;q=0.9,en;q=0.8,ru;q=0.7,fr;q=0.6")
                .header("Connection", "keep-alive")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Referer", "https://vstup.edbo.gov.ua")
                .header("Host", "vstup.edbo.gov.ua")
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
                .bodyToMono(String.class)
                .doOnError(e -> {
                    System.err.println("=== ERROR IN APPLICATION WEBCLIENT REQUEST for offerId=" + offerId + ", last=" + last + " ===");
                });
    }

    private void processAndMapApplication(ApplicationDto dto, Offer offer) {

        if (!decryptApplicationDto(dto, offer.getEdboId())) {
            System.err.println("Could not decrypt application for offerId: " + offer.getEdboId());
            return;
        }

        if (!applicationValid(dto)){
            System.err.println("Invalid application for offerId: " + offer.getEdboId());
            return;
        }

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
            System.err.println("Application already exists for student: " + dto.getStudentFullName() + " for offerId: " + offer.getEdboId());
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

        applicationKeys.add(generateApplicationKey(dto.getStudentFullName(), rawScoreSum, application.getPriority()));
        applicationsCache.add(application);
        applicationsSaved++;
    }

    private boolean decryptApplicationDto(ApplicationDto dto, Long offerId) {
        if (dto.getPrsId() == null || dto.getOrderNumber() == null) {
            System.err.println("Не вдалося розшифрувати дані: відсутні 'prsid' або 'n' для offerId=" + offerId);
            return false;
        }

        try {
            // Calculate dynamic part of key
            String dynamicPart = "v" + ((7500 - dto.getPrsId()) * dto.getOrderNumber());

            // Decrypt full name
            String decryptedName = VstupDataDecryptor.decrypt(dto.getStudentFullName(), dynamicPart);
            dto.setStudentFullName(decryptedName);

            // Decrypt priority and budget
            String decryptedPriority = VstupDataDecryptor.decrypt(dto.getPriorityAndBudgetRaw(), dynamicPart);
            dto.setPriorityAndBudgetRaw(decryptedPriority); // Тепер тут буде "1", "2", "К" і т.д.

            return true;
        } catch (Exception e) {
            System.err.println("Помилка розшифрування для offerId=" + offerId + ". Зашифроване ПІБ: " + dto.getStudentFullName());
            return false;
        }
    }

    private boolean applicationValid(ApplicationDto dto) {
        return !dto.getStudentFullName().isEmpty() && dto.getScore() != 0;
    }

    private String generateApplicationKey(String fullName, Double rawScore, Integer priority) {
        return fullName.toLowerCase() + "_" + rawScore + "_" + priority;
    }

    private boolean isApplicationAdded(String fullName, Double rawScore, Integer priority) {
        String key = generateApplicationKey(fullName, rawScore, priority);
        return applicationKeys.contains(key);
    }

}