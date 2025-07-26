package org.admissio.scraper.dto.offer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScoreStatisticsDto {
    @JsonProperty("c")
    private CompetitionScoreDto competitionScore;

    @Data
    public static class CompetitionScoreDto {
        @JsonProperty("km")
        private Double minCompScore; // Minimum competition score
    }
}