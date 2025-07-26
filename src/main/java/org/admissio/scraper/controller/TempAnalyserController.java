package org.admissio.scraper.controller;

import lombok.AllArgsConstructor;
import org.admissio.scraper.service.AnalyserService;
import org.admissio.scraper.service.ApplicationService;
import org.admissio.scraper.service.OfferService;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@AllArgsConstructor
public class TempAnalyserController implements CommandLineRunner {
    AnalyserService analyserService;
    ApplicationService applicationService;
    OfferService offerService;
    //private Flyway flyway;

    @Override
    public void run(String... args) {
        /*flyway.clean();
        flyway.migrate();*/

        //analyserService.analyse();

        offerService.scrapeOffers();

    }
}
