package org.admissio.scraper.service;

import org.admissio.scraper.entity.QuotaType;

public record ApplicationKey(Long offerId, QuotaType quotaType) {
}
