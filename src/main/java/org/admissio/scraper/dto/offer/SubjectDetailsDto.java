package org.admissio.scraper.dto.offer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SubjectDetailsDto {
    @JsonProperty("sn")
    private String subjectName; // e.g., "Українська мова", "Математика"
    @JsonProperty("mv")
    private Integer minScore; // Minimum score for this subject
    @JsonProperty("k")
    private Double subjectCoef;
}