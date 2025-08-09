package org.admissio.scraper.service;

import jakarta.annotation.PostConstruct;
import org.admissio.scraper.dto.offer.EdboResponseWrapper;
import org.admissio.scraper.dto.offer.OfferDetailsDto;
import org.admissio.scraper.dto.offer.SubjectDetailsDto;
import org.admissio.scraper.entity.Major;
import org.admissio.scraper.entity.Offer;
import org.admissio.scraper.entity.University;
import org.admissio.scraper.repository.OfferRepository;
import org.admissio.scraper.utils.WebClientInsecure;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OfferService {
    private final WebClient webClient;
    private MajorService majorService;
    private final String FULL_API_URL = "https://vstup.edbo.gov.ua/offers-list/";
    @Value("${scraper.edbo.api.cookie}")
    private String cookie;
    private final ObjectMapper jacksonObjectMapper;
    private OfferRepository offerRepository;
    private BatchSavingService batchSavingService;
    private static Map<Long, Offer> offersCache = new HashMap<>();
    private static List<Offer> offersBatch = new ArrayList<>();
    private int savedOffersCounter = 0;
    private int duplicateOffersCounter = 0;
    private final String highScoreMajorCodes[] = {"C3", "D4", "D8", "D9", "I1", "I2", "I3", "I4", "I8"};

    OfferService(ObjectMapper jacksonObjectMapper, MajorService majorService, OfferRepository offerRepository, BatchSavingService batchSavingService) {
        this.webClient = WebClientInsecure.createInsecureWebClient(FULL_API_URL);
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.majorService = majorService;
        this.offerRepository = offerRepository;
        this.batchSavingService = batchSavingService;
    }

    @PostConstruct
    public void init() {
        List<Offer> offers = (List<Offer>) offerRepository.findAll();
        for (Offer offer : offers) {
            offersCache.put(offer.getEdboId(), offer);
        }
    }

    public void scrapeOffers() {
        System.out.println("Start scraping offers");
        String[] allOffers = getAllOffers();
        System.out.println("Get " + allOffers.length + " batch offers from file");

        for (int i = 0; i < allOffers.length; i++) {
            System.out.println("Processing offer batch number: " + i);
            scrapeOffersBatch(allOffers[i], i);
            batchSavingService.saveOffersInBatch(offersBatch);
            offersBatch.clear();
            System.out.println("Number of saved offers: " + savedOffersCounter);
        }

        System.out.println("End scraping offers");
        System.err.println("Number of saved offers: " + savedOffersCounter);
        System.err.println("Number of duplicate offers: " + duplicateOffersCounter);

    }

    private void scrapeOffersBatch(String offerIds, int batchIndex) {
        String rawResponse = sendRequestForRawOffer(offerIds).block();

        if (rawResponse != null) {
            try {
                Thread.sleep(2000);
                EdboResponseWrapper edboResponse = jacksonObjectMapper.readValue(rawResponse, EdboResponseWrapper.class);

                if (edboResponse != null && edboResponse.getOffers() != null && !edboResponse.getOffers().isEmpty()) {
                    edboResponse.getOffers().forEach(this::processAndMapOffer);
                } else {
                    System.out.println("Parsed JSON is empty or null offers list for batch: " + batchIndex);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse raw response as JSON for the batch number [" + batchIndex + "] ");
                e.printStackTrace();
            }
        }
    }


    private Mono<String> sendRequestForRawOffer(String offerIds) {
        String body = "ids=" + offerIds;

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
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).map(Exception::new))
                .bodyToMono(String.class)
                .doOnError(e -> {
                    System.err.println("=== ERROR IN EDBO WEBCLIENT REQUEST (Raw Body) ===");
                });
    }

    private void processAndMapOffer(OfferDetailsDto offerDto) {

        if (offerDto == null || offerDto.getEdboUsid() == null) {
            System.err.println("OfferDetailsDto is null");
            return;
        }

        if (offersCache.get(offerDto.getEdboUsid()) != null) {
            System.err.println("Duplicate with id:" + offerDto.getEdboUsid());
            duplicateOffersCounter++;
            return;
        }

        if (offerDto.getAllPlaces() == null) {
            //System.err.println("OfferId: " + offerDto.getEdboUsid() + " doesn't have any places");
            return;
        }

        Offer offer = new Offer();

        offer.setEdboId(offerDto.getEdboUsid());
        String offerName = offerDto.getName();
        offer.setName(truncateString(offerName != null ? offerName : "Не вказано", 255));
        String facultyName = offerDto.getFacultyName();
        offer.setFaculty(truncateString(facultyName != null ? facultyName : "Не вказано", 255));
        String educationalProgramName = offerDto.getEducationalProgram();
        offer.setEducationalProgram(truncateString(educationalProgramName != null ? educationalProgramName : offer.getName(), 255));
        offer.setEducationForm(offerDto.getEducationFormName());
        int budgetPlaces = offerDto.getBudgetPlaces() != null ? offerDto.getBudgetPlaces() : 0;
        offer.setBudgetPlaces(budgetPlaces);
        offer.setContractPlaces(offerDto.getContractPlaces() != null ? offerDto.getContractPlaces() : offerDto.getAllPlaces());
        offer.setQuota1Places(budgetPlaces / 10); // minimum 10% of budget
        offer.setQuota2Places(budgetPlaces / 10); // minimum 10% of budget

        if (offer.getQuota1Places() < 1 && budgetPlaces > 0)
            offer.setQuota1Places(1);
        if (offer.getQuota2Places() < 1 && budgetPlaces > 0)
            offer.setQuota2Places(1);

        // Convert regionCoefString and price
        try {
            if (offerDto.getRegionCoefString() != null && offerDto.getPrice() != null) {
                offer.setRegionCoef(Double.parseDouble(offerDto.getRegionCoefString()));
                offer.setPrice(Integer.parseInt(offerDto.getPrice()));
            } else {
                offer.setRegionCoef(1.0); // Default value if not present
                offer.setPrice(0);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing regionCoef of price for ID " + offerDto.getEdboUsid());
            offer.setRegionCoef(1.0);
            offer.setPrice(0);
        }

        // Default values
        offer.setAdditionalPoints(0);
        offer.setMinCompetitionScore(100);

        // Extracting min scores from subjectDetailsMap (the 'os' field in JSON)
        try {
            setSubjectsMinScore(offerDto, offer);
            if (!checkValidSubjectsList(offer)){
                //System.err.println("Subject list is invalid for offerId: " + offerDto.getEdboUsid());
                return;
            }
        } catch (IllegalArgumentException e) {
            //System.err.println(e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println("Error in setSubjectsMinScore for offerId: " + offerDto.getEdboUsid());
            e.printStackTrace();
            return;
        }

        // Set min score
        offer.setMinApplicationScore(100);

        for (String code : highScoreMajorCodes) {
            if (offerDto.getMajorCode().equalsIgnoreCase(code)) {
                offer.setMinApplicationScore(150);
                break;
            }
        }

        // Get Major
        Major major = majorService.getMajor(offerDto.getDetailedMajorCode() != null ? offerDto.getDetailedMajorCode() : offerDto.getMajorCode());
        if (major != null) {
            offer.setMajor(major);
        } else {
            offer.setMajor(majorService.addMajor(offerDto));
        }

        University uni = UniversityService.getUniversityByCode(offerDto.getUniversityCode());

        if (uni == null) {
            System.err.println("Not found university to map with offerId:" + offerDto.getEdboUsid());
            return;
        }

        offer.setUniversity(uni);

        offersCache.put(offer.getEdboId(), offer);
        offersBatch.add(offer);
        savedOffersCounter++;

    }

    // Used to avoid errors with values that are too long for db
    private String truncateString(String text, int maxLength) {
        if (text != null && text.length() > maxLength) {
            return text.substring(0, maxLength);
        }
        return text;
    }

    private boolean checkValidSubjectsList(Offer offer) {
        return offer.getMinUkLangScore() != null && offer.getMinUkLangScore() != 0
                && offer.getMinMathScore() != null && offer.getMinMathScore() != 0;
    }

    private void setSubjectsMinScore(OfferDetailsDto offerDto, Offer offer) throws IllegalArgumentException {
        if (offerDto.getSubjectDetailsMap() != null) {
            for (SubjectDetailsDto subject : offerDto.getSubjectDetailsMap().values()) {
                if (!checkValidScore(subject)) {
                    throw new IllegalArgumentException("Invalid score for offerId:" + offerDto.getEdboUsid());
                }
                switch (subject.getSubjectName()) {
                    case "Українська мова":
                        offer.setMinUkLangScore(subject.getMinScore());
                        break;
                    case "Математика":
                        offer.setMinMathScore(subject.getMinScore());
                        break;
                    case "Історія України":
                        offer.setMinHistoryScore(subject.getMinScore());
                        break;
                    case "Українська література":
                        offer.setMinUkLitScore(subject.getMinScore());
                        break;
                    case "Іноземна мова":
                        offer.setMinForeignLangScore(subject.getMinScore());
                        break;
                    case "Біологія":
                        offer.setMinBiologyScore(subject.getMinScore());
                        break;
                    case "Географія":
                        offer.setMinGeographyScore(subject.getMinScore());
                        break;
                    case "Фізика":
                        offer.setMinPhysicsScore(subject.getMinScore());
                        break;
                    case "Хімія":
                        offer.setMinChemistryScore(subject.getMinScore());
                        break;
                    case "Бал за успішне закінчення підготовчих курсів закладу освіти":
                        offer.setAdditionalPoints(1);
                        break;
                    case "Творчий конкурс":
                        offer.setMinCompetitionScore(subject.getMinScore() != null ? subject.getMinScore() : 100);
                        break;
                    case "Співбесіда":
                        offer.setMinInterviewScore(subject.getMinScore());
                        break;
                    case "Мотиваційний лист":
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled subject: " + subject.getSubjectName() + " with score: " + subject.getMinScore()
                                + " id: " + offerDto.getEdboUsid());

                }
            }
        }
    }

    private boolean checkValidScore(SubjectDetailsDto subject) {
        return subject.getSubjectCoef() <= 1;
    }

    private String[] getAllOffers() {

        String xmlContent = readXmlFromLocalResource("offersData/sitemapvstup2025_2.xml");

        if (xmlContent == null || xmlContent.isEmpty()) {
            System.err.println("Failed to read sitemap XML from local resource.");
            return new String[0];
        }

        // Search for urls
        Pattern urlPattern = Pattern.compile("<loc>(.*?)</loc>");
        Matcher matcher = urlPattern.matcher(xmlContent);

        // Get ids from urls
        List<String> allOfferIds = new ArrayList<>();
        Pattern idPattern = Pattern.compile("/(\\d+)/$");

        while (matcher.find()) {
            String fullUrl = matcher.group(1);
            Matcher idMatcher = idPattern.matcher(fullUrl);
            if (idMatcher.find()) {
                allOfferIds.add(idMatcher.group(1));
            }
        }

        // Group batches
        List<String> result = new ArrayList<>();
        StringBuilder currentBatch = new StringBuilder();
        final int batchSize = 128;

        for (int i = 0; i < allOfferIds.size(); i++) {
            currentBatch.append(allOfferIds.get(i));
            if ((i + 1) % batchSize == 0 || i == allOfferIds.size() - 1) {
                result.add(currentBatch.toString());
                currentBatch = new StringBuilder();
            } else {
                currentBatch.append(",");
            }
        }

        return result.toArray(new String[0]);
    }

    private String readXmlFromLocalResource(String filePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                return null;
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error reading local XML file: " + filePath);
            e.printStackTrace();
            return null;
        }
    }

    public static List<Offer> getOffersCacheList() {
        return new ArrayList<>(offersCache.values());
    }

}