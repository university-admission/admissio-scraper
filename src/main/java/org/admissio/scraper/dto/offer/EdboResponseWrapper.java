package org.admissio.scraper.dto.offer;

import lombok.Data;

import java.util.List;

@Data
public class EdboResponseWrapper {
    // This top-level DTO only needs the "offers" list
    private List<OfferDetailsDto> offers;
}