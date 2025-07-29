package org.admissio.scraper.dto.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
public class ApplicationDto {

    // Для поля score
    @JsonProperty("kv")
    private Double score;

    // Для поля priority та isBudget
    @JsonProperty("p")
    private String priorityAndBudgetRaw; // Наприклад: "3 (Б)" або "2 (К)"

    // Для поля fio
    @JsonProperty("fio")
    private String studentFullName; // Відповідає полю fullName у Student

    // Для поля rawScore та quotaType
    @JsonProperty("rss")
    private List<RssEntryDto> rssEntries;

    // Додаткові поля з JSON, які можуть бути корисними для налагодження або майбутнього використання
    @JsonProperty("n")
    private Integer orderNumber; // Поле 'n' у JSON
    @JsonProperty("prsid")
    private Integer prsId; // Поле 'prsid' у JSON
    @JsonProperty("ptid")
    private Integer ptId; // Поле 'ptid' у JSON
    @JsonProperty("pa")
    private Integer pa; // Поле 'pa' у JSON
    @JsonProperty("d")
    private Integer d; // Поле 'd' у JSON
    @JsonProperty("cp")
    private Integer cp; // Поле 'cp' у JSON
    @JsonProperty("cpt")
    private String cpText; // Поле 'cpt' у JSON
    @JsonProperty("cpd")
    private String cpDate; // Поле 'cpd' у JSON
    @JsonProperty("artid")
    private Integer artId; // Поле 'artid' у JSON
}