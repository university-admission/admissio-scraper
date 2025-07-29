package org.admissio.scraper.dto.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RssEntryDto {
    @JsonProperty("kv")
    private String kv; // "+57.300"
    @JsonProperty("f")
    private String f;  // Example: "191 x 0.3"
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("t")
    private String type; // Example: "q" для квот
    @JsonProperty("sn")
    private String quotaName; // Example: "Квота 1", "Квота 2"
}