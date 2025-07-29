package org.admissio.scraper.dto.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class EdboApplicationResponseWrapper {
    @JsonProperty("requests")
    private List<ApplicationDto> requests;
}