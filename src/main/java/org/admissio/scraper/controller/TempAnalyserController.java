package org.admissio.scraper.controller;

import lombok.AllArgsConstructor;
import org.admissio.scraper.service.AnalyserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@AllArgsConstructor
public class TempAnalyserController implements CommandLineRunner {
    AnalyserService analyserService;

    @Override
    public void run(String... args) {
        analyserService.analyse();
    }
}
