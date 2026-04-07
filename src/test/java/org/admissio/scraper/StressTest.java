package org.admissio.scraper;

import lombok.extern.slf4j.Slf4j;
import org.admissio.scraper.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class StressTest extends AbstractAnalyserTest {
    @Test
    void performanceStressTest() {
        int OFFERS_COUNT = 40_000;
        int STUDENTS_COUNT = 167_000;
        int MAX_BUDGET_APPS = 10;
        int MAX_CONTRACT_APPS = 5;

        Random rand = new Random(12345);

        log.info("Data generation: 40k offers, 167k students, up to 15 apps per student...");

        List<Offer> offers = new ArrayList<>();
        for (int i = 0; i < OFFERS_COUNT; i++) {
            int budgetPlaces = 5 + rand.nextInt(35);
            int contractPlaces = budgetPlaces * 2;
            int quota1 = Math.max(1, budgetPlaces / 10);
            int quota2 = Math.max(1, budgetPlaces / 10);

            offers.add(createOffer("Offer " + i, budgetPlaces, contractPlaces, quota1, quota2));
        }

        long expectedAppCount = 0;
        for (int i = 0; i < STUDENTS_COUNT; i++) {
            double score = 120 + rand.nextDouble() * 80;
            Student s = createStudent("Student " + i, score);

            Set<Integer> usedOffers = new HashSet<>();

            int budgetAppsCount = 1 + rand.nextInt(MAX_BUDGET_APPS);
            for (int priority = 1; priority <= budgetAppsCount; priority++) {
                int offerIdx = rand.nextInt(OFFERS_COUNT);
                while (usedOffers.contains(offerIdx)) offerIdx = rand.nextInt(OFFERS_COUNT);
                usedOffers.add(offerIdx);

                QuotaType qt = QuotaType.GENERAL;
                int qChance = rand.nextInt(100);
                if (qChance < 5) qt = QuotaType.QUOTA_1;
                else if (qChance < 10) qt = QuotaType.QUOTA_2;

                createApplication(s, offers.get(offerIdx), priority, true, qt);
                expectedAppCount++;
            }

            int contractAppsCount = 1 + rand.nextInt(MAX_CONTRACT_APPS);
            for (int j = 1; j <= contractAppsCount; j++) {
                int offerIdx = rand.nextInt(OFFERS_COUNT);
                while (usedOffers.contains(offerIdx)) offerIdx = rand.nextInt(OFFERS_COUNT);
                usedOffers.add(offerIdx);

                int priority = budgetAppsCount + j;
                createApplication(s, offers.get(offerIdx), priority, false, QuotaType.GENERAL);
                expectedAppCount++;
            }

            if (i > 0 && i % 10000 == 0)
                log.info("Generated data for {} students...", i);
        }


        log.info("Data generation finished. Total expected applications: {}", expectedAppCount);
        log.info("Starting analysis...");

        long start = System.currentTimeMillis();
        analyserService.analyse();
        long duration = System.currentTimeMillis() - start;

        log.info("Results: ");
        log.info("Count of applications in DB: {}", applicationRepo.count());
        log.info("Analysis Duration: {} ms", duration);
    }
}