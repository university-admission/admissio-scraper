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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OfferService {
    private final WebClient webClient;
    private MajorService majorService;
    private static final String FULL_API_URL = "https://vstup.edbo.gov.ua/offers-list/";
    private final ObjectMapper jacksonObjectMapper;
    private OfferRepository offerRepository;
    public static List<Offer> offersCache;
    private BatchSavingService  batchSavingService;
    private final String highScoreMajorCodes[] = {"C3", "D4", "D8", "D9", "I1", "I2", "I3", "I4", "I8"};

    OfferService(ObjectMapper jacksonObjectMapper,
                 MajorService majorService,
                 OfferRepository offerRepository, BatchSavingService batchSavingService) {
        this.webClient = WebClientInsecure.createInsecureWebClient(FULL_API_URL);
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.majorService = majorService;
        this.offerRepository = offerRepository;
        this.batchSavingService = batchSavingService;
    }

    @PostConstruct
    public void init() {
        this.offersCache = (List<Offer>) offerRepository.findAll();
    }

    public void scrapeOffers(String offerIds, University university) {
        String rawResponse = sendRequestForRawOffer(offerIds).block();

        if (rawResponse != null) {
            try {
                EdboResponseWrapper edboResponse = jacksonObjectMapper.readValue(rawResponse, EdboResponseWrapper.class);

                if (edboResponse != null && edboResponse.getOffers() != null && !edboResponse.getOffers().isEmpty()) {
                    edboResponse.getOffers().forEach(offerDto -> processAndMapOffer(offerDto, university));
                } else {
                    System.out.println("Parsed JSON is empty or null offers list for IDs: " + offerIds);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse raw response as JSON for the batch of IDs [" + offerIds + "]: ");
            }
        }
    }


    private Mono<String> sendRequestForRawOffer(String offerIds) {
        String body = "ids=" + offerIds;

        return webClient.post()
                .uri(FULL_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://vstup.edbo.gov.ua")
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

    private void processAndMapOffer(OfferDetailsDto offerDto, University university) {
        //System.out.println("Processing offer DTO with EDBO ID: " + offerDto.getEdboUsid());

        if (isOfferAdded(offerDto.getEdboUsid())) {
            //System.err.println("Duplicate with id:" + offerDto.getEdboUsid());
            return;
        }

        Offer offer = new Offer();

        offer.setEdboId(offerDto.getEdboUsid());
        offer.setName(offerDto.getName() != null ? offerDto.getName() : "Не вказано");
        offer.setFaculty(offerDto.getFacultyName() != null ? offerDto.getFacultyName() : "Не вказано");
        offer.setEducationalProgram(offerDto.getEducationalProgram() != null ? offerDto.getEducationalProgram() : offer.getName());
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
        setSubjectsMinScore(offerDto, offer);

        // Set min score
        offer.setMinApplicationScore(100);

        for (String code : highScoreMajorCodes) {
            if (offerDto.getMajorCode().equalsIgnoreCase(code)) {
                offer.setMinApplicationScore(150);
                break;
            }
        }

        // Get Major
        Optional<Major> majorOptional = majorService.getMajor(offerDto.getMajorCode());
        if (majorOptional.isPresent()) {
            offer.setMajor(majorOptional.get());
        } else {
            offer.setMajor(majorService.addMajor(offerDto));
        }

        offer.setUniversity(university);

        offersCache.add(offer);

    }

    private void setSubjectsMinScore(OfferDetailsDto offerDto, Offer offer) {
        if (offerDto.getSubjectDetailsMap() != null) {
            for (SubjectDetailsDto subject : offerDto.getSubjectDetailsMap().values()) {
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
                        offer.setMinCompetitionScore(subject.getMinScore());
                        break;
                    case "Мотиваційний лист":
                        break;
                    default:
                        // Log or handle subjects that are not mapped to entity
                        System.err.println("Unhandled subject: " + subject.getSubjectName() + " with score: " + subject.getMinScore());
                }
            }
        }
    }

    private boolean isOfferAdded(Long offerEdboId){
        for (Offer offer : offersCache) {
            if (offer.getEdboId().equals(offerEdboId)) {
                return true;
            }
        }
        return false;
    }

}
