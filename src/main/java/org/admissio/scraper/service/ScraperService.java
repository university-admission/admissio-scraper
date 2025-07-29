package org.admissio.scraper.service;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ScraperService {
    @NonNull
    private UniversityRegionService universityRegionService;
    @NonNull
    private BatchSavingService batchSavingService;
    @NonNull
    private ApplicationService applicationService;
    private final String[] informaticsMajors = {"F1", "F2", "F3", "F4", "F5", "F6", "F7"};
    private final String[] test = {"F1", "B1"};
    private final String[] majorCodes = {
            "A1", "A2", "A3", "A4", "A5", "A6", "A7", "B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9",
            "B10", "B11", "B12", "B13", "B14", "C1", "C2", "C3", "C4", "C5", "C6", "C7", "D1", "D2",
            "D3", "D4", "D5", "D7", "D8", "D9", "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8",
            "F1", "F2", "F3", "F4", "F5", "F6", "F7", "G1", "G2", "G3", "G4", "G5", "G6", "G7",
            "G8", "G9", "G10", "G11", "G12", "G13", "G14", "G15", "G16", "G17", "G18", "G19",
            "G20", "G21", "G22", "H1", "H2", "H3", "H4", "H5", "H6", "H7", "I1", "I2", "I3",
            "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I11", "J1", "J2", "J3", "J4", "J5", "J6",
            "J7", "J8", "K1", "K2", "K3", "K4", "K5", "K6", "k7", "K8", "K9", "K10"
    };


    public void updateApplications(){
        applicationService.scrapeApplications();
        batchSavingService.saveStudentsInBatch(StudentService.studentsCache);
        batchSavingService.saveApplicationsInBatch(ApplicationService.applicationsCache);
        clearCache();
    }

    public void scrapeAllData(){
        universityRegionService.scrapeUniversitiesByRegion(test);
        batchSavingService.saveUniversitiesInBatch(UniversityService.universitiesCache);
        batchSavingService.saveMajorsInBatch(MajorService.majorsCache);
        batchSavingService.saveOffersInBatch(OfferService.offersCache);
        updateApplications();
    }

    private void clearCache(){
        ApplicationService.applicationsCache.clear();
        StudentService.studentsCache.clear();
        OfferService.offersCache.clear();
        MajorService.majorsCache.clear();
        UniversityService.universitiesCache.clear();
    }

}
