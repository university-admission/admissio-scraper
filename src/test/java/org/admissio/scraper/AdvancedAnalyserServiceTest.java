package org.admissio.scraper;

import org.admissio.scraper.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Assertions;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public class AdvancedAnalyserServiceTest extends AbstractAnalyserTest {

    @Test
    void testLongChainDisplacement() {
        Offer uniA = createOffer("Uni A", 1, 0, 0, 0);
        Offer uniB = createOffer("Uni B", 1, 0, 0, 0);
        Offer uniC = createOffer("Uni C", 1, 0, 0, 0);
        Offer uniD = createOffer("Uni D", 1, 0, 0, 0);

        Student sGod = createStudent("God (200)", 200.0);
        Student sKing = createStudent("King (190)", 190.0);
        Student sLord = createStudent("Lord (180)", 180.0);
        Student sPeasant = createStudent("Peasant (170)", 170.0);
        Student sBeggar = createStudent("Beggar (160)", 160.0);

        createApplication(sGod, uniA, 1, true, QuotaType.GENERAL);

        createApplication(sKing, uniA, 1, true, QuotaType.GENERAL);
        createApplication(sKing, uniB, 2, true, QuotaType.GENERAL);

        createApplication(sLord, uniB, 1, true, QuotaType.GENERAL);
        createApplication(sLord, uniC, 2, true, QuotaType.GENERAL);

        createApplication(sPeasant, uniC, 1, true, QuotaType.GENERAL);
        createApplication(sPeasant, uniD, 2, true, QuotaType.GENERAL);

        createApplication(sBeggar, uniD, 1, true, QuotaType.GENERAL);

        analyserService.analyse();

        List<Application> apps = (List<Application>) applicationRepo.findAll();

        assertStatus(sGod, uniA, true, apps);

        assertStatus(sKing, uniA, false, apps);
        assertStatus(sKing, uniB, true, apps);

        assertStatus(sLord, uniB, false, apps);
        assertStatus(sLord, uniC, true, apps);

        assertStatus(sPeasant, uniC, false, apps);
        assertStatus(sPeasant, uniD, true, apps);

        assertStatus(sBeggar, uniD, false, apps);
    }

    @Test
    void testQuotaVsGeneral() {
        Offer offer = createOffer("Mixed Offer", 1, 0, 1, 0);

        Student sGenHigh = createStudent("General High (190)", 190.0);
        Student sGenMid = createStudent("General Mid (180)", 180.0);
        Student sQuotaLow = createStudent("Quota Low (150)", 150.0);

        createApplication(sGenHigh, offer, 1, true, QuotaType.GENERAL);
        createApplication(sGenMid, offer, 1, true, QuotaType.GENERAL);
        createApplication(sQuotaLow, offer, 1, true, QuotaType.QUOTA_1);

        analyserService.analyse();

        List<Application> apps = (List<Application>) applicationRepo.findAll();

        assertStatus(sGenHigh, offer, true, apps);
        assertStatus(sGenMid, offer, false, apps);
        assertStatus(sQuotaLow, offer, true, apps);
    }

    @Test
    void testCascadeFailureToContract() {
        Offer uniTop = createOffer("Top (Budget Only)", 1, 0, 0, 0);
        Offer uniMid = createOffer("Mid (Budget Only)", 1, 0, 0, 0);
        Offer uniSafety = createOffer("Safety (Contract)", 0, 10, 0, 0);

        Student sGenius1 = createStudent("Genius 1", 200.0);
        Student sGenius2 = createStudent("Genius 2", 195.0);

        Student sYou = createStudent("You", 180.0);

        createApplication(sGenius1, uniTop, 1, true, QuotaType.GENERAL);
        createApplication(sGenius2, uniMid, 1, true, QuotaType.GENERAL);

        createApplication(sYou, uniTop, 1, true, QuotaType.GENERAL);
        createApplication(sYou, uniMid, 2, true, QuotaType.GENERAL);
        createApplication(sYou, uniSafety, 6, false, QuotaType.GENERAL);

        analyserService.analyse();

        List<Application> apps = (List<Application>) applicationRepo.findAll();

        assertStatus(sYou, uniTop, false, apps);
        assertStatus(sYou, uniMid, false, apps);
        assertStatus(sYou, uniSafety, true, apps);
    }

    private void assertStatus(Student s, Offer o, boolean shouldBeCounted, List<Application> apps) {
        boolean isCounted = apps.stream()
                .filter(a -> a.getStudent().getId().equals(s.getId()) && a.getOffer().getId().equals(o.getId()))
                .findFirst()
                .map(Application::getIsCounted)
                .orElse(false);

        if (shouldBeCounted) {
            Assertions.assertTrue(isCounted,
                    String.format("Студент %s мав пройти в %s", s.getFullName(), o.getName()));
        } else {
            Assertions.assertFalse(isCounted,
                    String.format("Студент %s НЕ мав пройти в %s", s.getFullName(), o.getName()));
        }
    }
}
