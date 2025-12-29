package org.admissio.scraper;

import org.admissio.scraper.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

@SpringBootTest
@ActiveProfiles("test")
public class StressTest extends AbstractAnalyserTest {
    @Test
    void testLargeRandomCompetition() {
        int STUDENTS_COUNT = 100;
        int OFFERS_COUNT = 5;
        int PLACES_PER_OFFER = 15;

        List<Offer> offers = new ArrayList<>();
        for (int i = 0; i < OFFERS_COUNT; i++) {
            offers.add(createOffer("Uni " + (char)('A' + i), PLACES_PER_OFFER, 50, 0, 0));
        }

        List<Student> students = new ArrayList<>();
        Random rand = new Random(12345);

        for (int i = 0; i < STUDENTS_COUNT; i++) {
            double score = 100 + rand.nextDouble() * 100;
            students.add(createStudent("Student " + i, score));
        }

        for (Student s : students) {
            List<Offer> shuffledOffers = new ArrayList<>(offers);
            Collections.shuffle(shuffledOffers, rand);

            int appsCount = 3 + rand.nextInt(3);

            for (int priority = 1; priority <= appsCount; priority++) {
                Offer offer = shuffledOffers.get(priority - 1);
                createApplication(s, offer, priority, true, QuotaType.GENERAL);
            }
        }

        System.out.println("Починаємо аналіз " + STUDENTS_COUNT + " студентів...");
        long start = System.currentTimeMillis();

        analyserService.analyse();

        long duration = System.currentTimeMillis() - start;
        System.out.println("Аналіз завершено за " + duration + " мс.");

        List<Application> allApps = (List<Application>) applicationRepo.findAll();

        for (Student s : students)
            validateStudentResult(s, allApps);
    }

    private void validateStudentResult(Student s, List<Application> allApps) {
        List<Application> myApps = allApps.stream()
                .filter(a -> a.getStudent().getId().equals(s.getId()))
                .sorted(Comparator.comparingInt(Application::getPriority))
                .toList();

        Optional<Application> passedApp = myApps.stream()
                .filter(Application::getIsCounted)
                .findFirst();

        if (passedApp.isPresent()) {
            Application passed = passedApp.get();

            for (Application app : myApps) {
                if (app.getPriority() < passed.getPriority()) {
                    Assertions.assertFalse(app.getIsCounted(),
                            "Студент пройшов на нижчий пріоритет, хоча пройшов і на вищий!");

                    assertRejectionJustified(app, s, allApps);
                } else if (app.getPriority() > passed.getPriority()) {
                    Assertions.assertFalse(app.getIsCounted(),
                            "Студент пройшов на кілька пріоритетів одночасно!");
                }
            }
        } else {
            for (Application app : myApps) {
                Assertions.assertFalse(app.getIsCounted());
                assertRejectionJustified(app, s, allApps);
            }
        }
    }

    private void assertRejectionJustified(Application rejectedApp, Student me, List<Application> allApps) {
        Offer offer = rejectedApp.getOffer();

        List<Application> acceptedApps = allApps.stream()
                .filter(a -> a.getOffer().getId().equals(offer.getId()))
                .filter(Application::getIsCounted)
                .filter(Application::getIsBudget)
                .toList();

        if (acceptedApps.size() < offer.getBudgetPlaces()) {
            Assertions.fail(String.format(
                    "СКАНДАЛ! Студента %s (Бал: %.2f) відхилено з %s, хоча там є вільні місця (%d/%d) і він не пройшов на вищий пріоритет.",
                    me.getFullName(), me.getRawScore(), offer.getName(), acceptedApps.size(), offer.getBudgetPlaces()
            ));
        }

        double minScoreInUni = acceptedApps.stream()
                .mapToDouble(Application::getScore)
                .min()
                .orElse(Double.MAX_VALUE);

        if (me.getRawScore() > minScoreInUni) {
            Assertions.fail(String.format(
                    "НЕСПРАВЕДЛИВІСТЬ! Студента %s (Бал: %.2f) відхилено з %s, але туди взяли когось з балом %.2f",
                    me.getFullName(), me.getRawScore(), offer.getName(), minScoreInUni
            ));
        }
    }
}