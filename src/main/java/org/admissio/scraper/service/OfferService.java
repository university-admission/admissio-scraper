package org.admissio.scraper.service;

import org.admissio.scraper.dto.offer.EdboResponseWrapper;
import org.admissio.scraper.dto.offer.OfferDetailsDto;
import org.admissio.scraper.dto.offer.SubjectDetailsDto;
import org.admissio.scraper.entity.Major;
import org.admissio.scraper.entity.Offer;
import org.admissio.scraper.entity.University;
import org.admissio.scraper.repository.OfferRepository;
import org.admissio.scraper.utils.WebClientInsecure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

@Service
public class OfferService {
    private final WebClient webClient;
    private MajorService majorService;
    private UniversityService universityService;
    private static final String FULL_API_URL = "https://vstup.edbo.gov.ua/offers-list/";
    private final ObjectMapper jacksonObjectMapper;
    private OfferRepository offerRepository;
    private final String highScoreMajorCodes[] = {"C3", "D4", "D8", "D9", "I1", "I2", "I3", "I4", "I8"};
    private final int offerIdArr[] = {1472870};

    OfferService(ObjectMapper jacksonObjectMapper,
                 MajorService majorService,
                 UniversityService universityService,
                 OfferRepository offerRepository) {
        this.webClient = WebClientInsecure.createInsecureWebClient(FULL_API_URL);
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.majorService = majorService;
        this.universityService = universityService;
        this.offerRepository = offerRepository;
    }

    public void scrapeOffers() {

        for (int id : offerIdArr) {
            Mono<String> rawResponseMono = sendRequestForRawBody(id);

            rawResponseMono.subscribe(
                    rawResponse -> {
                        try {
                            // Manually parse the String as JSON using ObjectMapper
                            EdboResponseWrapper edboResponse = jacksonObjectMapper.readValue(rawResponse, EdboResponseWrapper.class);

                            if (edboResponse != null && edboResponse.getOffers() != null && !edboResponse.getOffers().isEmpty()) {
                                System.out.println("Received EdboResponseWrapper with " + edboResponse.getOffers().size() + " offers.");
                                edboResponse.getOffers().forEach(this::processAndMapOffer);
                            } else {
                                System.out.println("Parsed JSON is empty or null offers list for ID: " + id);
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to parse raw response as JSON for ID " + id + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    },
                    error -> System.err.println("Error during raw request or parsing for ID " + id + ": " + error.getMessage())
            );
        }
    }


    private Mono<String> sendRequestForRawBody(int offerId) {
        String body = "ids=" + offerId;

        return webClient.post()
                .uri(FULL_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://vstup.edbo.gov.ua")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).map(Exception::new))
                .bodyToMono(String.class)
                .doOnError(e -> {
                    System.err.println("=== ERROR IN EDBO WEBCLIENT REQUEST (Raw Body) ===");
                    e.printStackTrace();
                });
    }

    private void processAndMapOffer(OfferDetailsDto offerDto) {
        System.out.println("Processing offer DTO with EDBO ID: " + offerDto.getEdboUsid());

        Offer offer = new Offer();

        offer.setEdboId(offerDto.getEdboUsid());
        offer.setName(offerDto.getName() != null ? offerDto.getName() : offerDto.getMajorCode());
        offer.setFaculty(offerDto.getFacultyName());
        offer.setEducationForm(offerDto.getEducationFormName());
        offer.setBudgetPlaces(offerDto.getBudgetPlaces() != null ? offerDto.getBudgetPlaces() : 0);
        offer.setContractPlaces(offerDto.getContractPlaces() != null ? offerDto.getContractPlaces() : 0);
        offer.setQuota1Places(offerDto.getBudgetPlaces() / 10); // minimum 10% of budget
        offer.setQuota2Places(offerDto.getBudgetPlaces() / 10); // minimum 10% of budget

        if (offer.getQuota1Places() < 1 && offer.getBudgetPlaces() > 0)
            offer.setQuota1Places(1);
        if (offer.getQuota2Places() < 1 && offer.getBudgetPlaces() > 0)
            offer.setQuota2Places(1);

        // Convert regionCoefString to Double
        try {
            if (offerDto.getRegionCoefString() != null) {
                offer.setRegionCoef(Double.parseDouble(offerDto.getRegionCoefString()));
            } else {
                offer.setRegionCoef(1.0); // Default value if not present
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing regionCoef for ID " + offerDto.getEdboUsid() + ": " + offerDto.getRegionCoefString());
            offer.setRegionCoef(1.0);
        }


        // Extracting min scores from subjectDetailsMap (the 'os' field in JSON)
        setSubjectsMinScore(offerDto, offer);

        // Set min score
        offer.setMinCompetitionScore(100);

        for (String code: highScoreMajorCodes){
            if (offerDto.getMajorCode().equalsIgnoreCase(code)) {
                offer.setMinCompetitionScore(150);
                break;
            }
        }

        offer.setAdditionalPoints(0);

        // Get Major and Uni from db
        try {
            Major major = getMajorByCode(offerDto.getMajorCode());
            offer.setMajor(major);
        }catch (Exception e){
            System.err.println(e.getMessage() + " for edbo_id "+offerDto.getEdboUsid());
            return;
        }

        try {
            University uni = getUniversityByCode(offerDto.getUniversityCode());
            offer.setUniversity(uni);
        }catch (Exception e){
            System.err.println(e.getMessage() + " for edbo_id "+offerDto.getEdboUsid());
            return;
        }

        offerRepository.save(offer);

    }

    private void setSubjectsMinScore(OfferDetailsDto offerDto, Offer offer){
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
                    case "Мотиваційний лист":
                        break;
                    default:
                        // Log or handle subjects that are not mapped to your entity
                        System.err.println("Unhandled subject: " + subject.getSubjectName() + " with score: " + subject.getMinScore());
                }
            }
        }
    }

    private Major getMajorByCode(String majorCode) throws Exception {
        Optional<Major> major = majorService.findMajorByCode(majorCode);
        if (major.isPresent()) {
            return major.get();
        }else {
            throw new Exception("Major code " + majorCode + " not found in db");
        }
    }

    private University getUniversityByCode(Integer universityCode) throws Exception {
        Optional<University> university = universityService.findByUniversityCode(universityCode);
        if (university.isPresent()) {
            return university.get();
        }else {
            throw new Exception("University code " + universityCode + " not found in db");
        }
    }

}
