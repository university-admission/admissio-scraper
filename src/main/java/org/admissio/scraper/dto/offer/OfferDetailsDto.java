package org.admissio.scraper.dto.offer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

// This DTO contains only the fields directly relevant to your Offer entity
// plus the nested structures for subjects and scores.
@Data
public class OfferDetailsDto {
    @JsonProperty("usid")
    private Long edboUsid; // Corresponds to Offer.edboId

    @JsonProperty("ssn") // Speciality name
    private String name; // Corresponds to Offer.name (or part of it)

    @JsonProperty("szn") // Detailed major name
    private String detailedName; // Corresponds to Major.name

    @JsonProperty("spn") // Education program
    private String educationalProgram; // Corresponds to Offer.educationalProgram

    @JsonProperty("ssc") // Code
    private String majorCode; // Corresponds to Offer.Major

    @JsonProperty("szc") // Full code
    private String detailedMajorCode; // Corresponds to Major.Major

    @JsonProperty("un") // University name
    private String universityName;

    @JsonProperty("uid")
    private Integer universityCode;

    @JsonProperty("ufn") // Faculty name
    private String facultyName; // Corresponds to Offer.faculty

    @JsonProperty("efn") // Education Form name (e.g., "Денна")
    private String educationFormName; // Corresponds to Offer.educationForm

    @JsonProperty("price") // price for a year
    private String price; // Corresponds to Offer.price

    @JsonProperty("ol")
    private Integer allPlaces;

    @JsonProperty("ox") // Budget places
    private Integer budgetPlaces; // Corresponds to Offer.budgetPlaces

    @JsonProperty("oc") // Contract places
    private Integer contractPlaces; // Corresponds to Offer.contractPlaces

    @JsonProperty("rk") // Region coefficient
    private String regionCoefString; // In JSON, it's "1", so parse as String, then convert to Double

    @JsonProperty("os") // Nested object for subject details (where min scores are)
    private Map<String, SubjectDetailsDto> subjectDetailsMap; // Key is an internal ID, value is SubjectDetailsDto

    @JsonProperty("st") // Nested object for statistics (where min competition score is)
    private ScoreStatisticsDto scoreStatistics;

}