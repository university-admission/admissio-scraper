package org.admissio.scraper.dto.university;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OsvitaUniversityDto {
    private String universityFullName;
    private Integer universityId;
}
