package org.admissio.scraper.controller;

import lombok.AllArgsConstructor;
import org.admissio.scraper.service.AnalyserService;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@AllArgsConstructor
public class TempAnalyserController implements CommandLineRunner {
    AnalyserService analyserService;
    private Flyway flyway;

    @Override
    public void run(String... args) {
        flyway.clean();
        flyway.migrate();

        analyserService.analyse();
    }
}
