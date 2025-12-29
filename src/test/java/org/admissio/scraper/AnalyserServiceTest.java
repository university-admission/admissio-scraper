package org.admissio.scraper;

import org.admissio.scraper.entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public class AnalyserServiceTest extends AbstractAnalyserTest {
    @Test
    void testBasicBudgetCompetition() {
        Offer offer = createOffer("CS-Kyiv", 1, 0, 0, 0);

        Student winner = createStudent("Winner", 190.0);
        Student loser = createStudent("Loser", 150.0);

        createApplication(winner, offer, 1, true, QuotaType.GENERAL);
        createApplication(loser, offer, 1, true, QuotaType.GENERAL);

        analyserService.analyse();

        List<Application> apps = (List<Application>) applicationRepo.findAll();
        assertStatus(winner, offer, true, apps);
        assertStatus(loser, offer, false, apps);
    }

    @Test
    void testPriorityHandling() {
        Offer topPriorityOffer = createOffer("Top Uni", 5, 0, 0, 0);
        Offer lowPriorityOffer = createOffer("Safe Uni", 5, 0, 0, 0);

        Student student = createStudent("Student", 180.0);

        createApplication(student, topPriorityOffer, 1, true, QuotaType.GENERAL);
        createApplication(student, lowPriorityOffer, 2, true, QuotaType.GENERAL);

        analyserService.analyse();

        List<Application> apps = (List<Application>) applicationRepo.findAll();

        assertStatus(student, topPriorityOffer, true, apps);
        assertStatus(student, lowPriorityOffer, false, apps);
    }

    @Test
    void testRecursiveDominoEffect() {
        Offer offerTop = createOffer("Top Uni", 1, 0, 0, 0);
        Offer offerMid = createOffer("Mid Uni", 1, 0, 0, 0);

        Student genius = createStudent("Genius", 190.0);
        Student you = createStudent("You", 180.0);

        createApplication(genius, offerTop, 1, true, QuotaType.GENERAL);
        createApplication(genius, offerMid, 2, true, QuotaType.GENERAL);

        createApplication(you, offerMid, 1, true, QuotaType.GENERAL);

        analyserService.analyse();

        List<Application> apps = (List<Application>) applicationRepo.findAll();

        assertStatus(genius, offerTop, true, apps);
        assertStatus(you, offerMid, true, apps);
    }

    @Test
    void testBudgetBlocksContract() {
        Offer offerBudget = createOffer("Uni Budget", 10, 0, 0, 0);
        Offer offerContract = createOffer("Uni Contract", 0, 10, 0, 0);

        Student student = createStudent("Rich Smart Student", 180.0);

        createApplication(student, offerBudget, 1, true, QuotaType.GENERAL);
        createApplication(student, offerContract, 6, false, QuotaType.GENERAL);

        analyserService.analyse();

        List<Application> apps = (List<Application>) applicationRepo.findAll();

        assertStatus(student, offerBudget, true, apps);
        assertStatus(student, offerContract, false, apps);
    }

    @Test
    void testContractFallback() {
        Offer offer = createOffer("Uni", 0, 5, 0, 0); // 0 бюджетних, 5 контрактних

        Student student = createStudent("Student", 150.0);

        createApplication(student, offer, 1, true, QuotaType.GENERAL);
        createApplication(student, offer, 6, false, QuotaType.GENERAL);

        analyserService.analyse();

        List<Application> apps = (List<Application>) applicationRepo.findAll();

        Application budgetApp = findApp(student, offer, true, apps);
        Assertions.assertFalse(budgetApp.getIsCounted(), "Мав не пройти на бюджет");

        Application contractApp = findApp(student, offer, false, apps);
        Assertions.assertTrue(contractApp.getIsCounted(), "Мав пройти на контракт");
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

    private Application findApp(Student s, Offer o, boolean isBudget, List<Application> apps) {
        return apps.stream()
                .filter(a -> a.getStudent().getId().equals(s.getId())
                        && a.getOffer().getId().equals(o.getId())
                        && a.getIsBudget() == isBudget)
                .findFirst()
                .orElseThrow();
    }
}
