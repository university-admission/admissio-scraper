package org.admissio.scraper.controller;

import lombok.AllArgsConstructor;
import org.admissio.scraper.entity.UniversityRegion;
import org.admissio.scraper.service.*;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@AllArgsConstructor
public class TempAnalyserController implements CommandLineRunner {
    AnalyserService analyserService;
    UniversityRegionService universityRegionService;
    //private Flyway flyway;

    @Override
    public void run(String... args) {
        /*flyway.clean();
        flyway.migrate();*/

        //analyserService.analyse();

        System.out.println("Start of scrapping!");
        long startTime = System.currentTimeMillis();

        universityRegionService.scrapeUniversitiesByRegion()
                .doOnSuccess(voidResult -> System.out.println("Scraping of universities by region completed successfully!"))
                .doOnError(throwable -> System.err.println("Error while scraping universities by region: " + throwable.getMessage()))
                .block();

        long totalDurationMillis  = System.currentTimeMillis() - startTime;
        System.out.println("Scraping of universities by region completed successfully!");
        long hours = totalDurationMillis / (1000 * 60 * 60);
        long minutes = (totalDurationMillis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = ((totalDurationMillis % (1000 * 60 * 60)) % (1000 * 60)) / 1000;
        long milliseconds = totalDurationMillis % 1000;
        System.out.printf("Total duration: %d hours, %d minutes, %d seconds, %d milliseconds.%n",
                hours, minutes, seconds, milliseconds);

    }
}
