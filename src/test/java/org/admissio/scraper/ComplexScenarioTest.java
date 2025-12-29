package org.admissio.scraper;

import org.admissio.scraper.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public class ComplexScenarioTest extends AbstractAnalyserTest {

    @Test
    void testMassiveStratification() {
        Offer uniTop = createOffer("Top Uni (Elite)", 2, 0, 0, 0);
        Offer uniMid = createOffer("Mid Uni (Average)", 2, 0, 0, 0);
        Offer uniLow = createOffer("Low Uni (Basic)", 2, 50, 0, 0); // Тут є контракт

        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            double score = 200.0 - (i * 10);
            students.add(createStudent("Student " + (int)score, score));
        }

        for (Student s : students) {
            createApplication(s, uniTop, 1, true, QuotaType.GENERAL);
            createApplication(s, uniMid, 2, true, QuotaType.GENERAL);
            createApplication(s, uniLow, 3, true, QuotaType.GENERAL);
            createApplication(s, uniLow, 6, false, QuotaType.GENERAL);
        }

        analyserService.analyse();

        List<Application> allApps = (List<Application>) applicationRepo.findAll();

        assertStatus(students.get(0), uniTop, true, allApps);
        assertStatus(students.get(1), uniTop, true, allApps);

        assertStatus(students.get(2), uniTop, false, allApps);
        assertStatus(students.get(2), uniMid, true, allApps);

        assertStatus(students.get(3), uniTop, false, allApps);
        assertStatus(students.get(3), uniMid, true, allApps);

        assertStatus(students.get(4), uniMid, false, allApps);
        assertStatus(students.get(4), uniLow, true, allApps);

        assertStatus(students.get(5), uniMid, false, allApps);
        assertStatus(students.get(5), uniLow, true, allApps);

        for (int i = 6; i < 10; i++) {
            Student s = students.get(i);
            assertStatus(s, uniLow, false, allApps);

            boolean contractPassed = allApps.stream()
                    .anyMatch(a -> a.getStudent().getId().equals(s.getId())
                            && a.getOffer().getId().equals(uniLow.getId())
                            && !a.getIsBudget()
                            && a.getIsCounted());

            Assertions.assertTrue(contractPassed,
                    "Студент " + s.getFullName() + " мав пройти на контракт, але не пройшов.");
        }
    }

    @Test
    void testCrowdedQuotaInteraction() {
        Offer offer = createOffer("Tight Offer", 1, 0, 1, 0);

        Student sGenTop = createStudent("Genius General (195)", 195.0);
        Student sGenMid = createStudent("Average General (180)", 180.0);

        Student sQuotaTop = createStudent("Genius Quota (190)", 190.0);
        Student sQuotaLow = createStudent("Low Quota (150)", 150.0);

        createApplication(sGenTop, offer, 1, true, QuotaType.GENERAL);
        createApplication(sGenMid, offer, 1, true, QuotaType.GENERAL);

        createApplication(sQuotaTop, offer, 1, true, QuotaType.QUOTA_1);
        createApplication(sQuotaLow, offer, 1, true, QuotaType.QUOTA_1);

        analyserService.analyse();
        List<Application> apps = (List<Application>) applicationRepo.findAll();

        assertStatus(sGenTop, offer, true, apps);
        assertStatus(sQuotaTop, offer, true, apps);
        assertStatus(sGenMid, offer, false, apps);
        assertStatus(sQuotaLow, offer, false, apps);
    }

    private void assertStatus(Student s, Offer o, boolean shouldBeCounted, List<Application> apps) {
        boolean isCounted = apps.stream()
                .filter(a -> a.getStudent().getId().equals(s.getId())
                        && a.getOffer().getId().equals(o.getId())
                        && a.getIsBudget())
                .findFirst()
                .map(Application::getIsCounted)
                .orElse(false);

        Assertions.assertEquals(shouldBeCounted, isCounted,
                String.format("Студент %s: очікувався статус бюджету %s в %s",
                        s.getFullName(), shouldBeCounted, o.getName()));
    }
}