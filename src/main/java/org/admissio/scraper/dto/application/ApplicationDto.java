package org.admissio.scraper.dto.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
public class ApplicationDto {

    @JsonProperty("kv")
    private Double score;

    // Для поля priority та isBudget
    @JsonProperty("p")
    private String priorityAndBudgetRaw; // Наприклад: "3 (Б)" або "2 (К)"

    @JsonProperty("fio")
    private String studentFullName;

    // Для поля rawScore та quotaType
    @JsonProperty("rss")
    private List<RssEntryDto> rssEntries;

    @JsonProperty("n")
    private Integer orderNumber;
    @JsonProperty("prsid")
    private Integer prsId;
    @JsonProperty("ptid")
    private Integer ptId;
    @JsonProperty("pa")
    private Integer pa;
    @JsonProperty("d")
    private Integer d;
    @JsonProperty("cp")
    private Integer cp;
    @JsonProperty("cpt")
    private String cpText;
    @JsonProperty("cpd")
    private String cpDate;
    @JsonProperty("artid")
    private Integer artId;
}