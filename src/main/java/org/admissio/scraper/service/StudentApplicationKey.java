package org.admissio.scraper.service;

import org.admissio.scraper.entity.QuotaType;

public record StudentApplicationKey(Long studentId, Double rawScore, QuotaType quotaType) {
}
