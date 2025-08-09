package org.admissio.scraper.dto.url;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UrlDto {
    @JsonProperty("url")
    private String url;
}
