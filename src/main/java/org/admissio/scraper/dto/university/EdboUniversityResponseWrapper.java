package org.admissio.scraper.dto.university;

import lombok.Data;

import java.util.List;

@Data
public class EdboUniversityResponseWrapper {
    private List<UniversityDto> universities;
}