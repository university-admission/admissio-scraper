package org.admissio.scraper.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.admissio.scraper.entity.*;
import org.admissio.scraper.repository.*;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyserService {
    @NonNull
    ApplicationRepository applicationRepository;
    @NonNull
    OfferRepository offerRepository;
    @NonNull
    EntityManager entityManager;
    @NonNull
    JdbcTemplate jdbcTemplate;

    private List<Application> allApplications;
    private List<Offer> allOffers;
    private Map<ApplicationKey, List<Application>> budgetApplicationsByKey;
    private Map<Long, List<Application>> contractApplicationsByOfferId;

    @Transactional
    public void analyse() {
        log.info("Starting analyser service");
        long start = System.currentTimeMillis();
        loadDataToMemory();
        analyseData();
        saveData();

        long duration = (System.currentTimeMillis() - start) / 1000;
        log.info("Application analysed for {} seconds.", duration);
    }

    //region Database
    private void saveData() {
        offerRepository.saveAll(allOffers);
        updateApplications(allApplications);
        entityManager.flush();
    }

    private void updateApplications(List<Application> applications) {
        String sql = "UPDATE applications SET is_counted = ?, is_checked = ?, is_actual = ? WHERE id = ?";

        int batchSize = 1000;

        for (int i = 0; i < applications.size(); i += batchSize) {
            List<Application> batch = applications.subList(i, Math.min(i + batchSize, applications.size()));

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                    Application app = batch.get(i);
                    ps.setBoolean(1, app.getIsCounted());
                    ps.setBoolean(2, app.getIsChecked());
                    ps.setBoolean(3, app.getIsActual());
                    ps.setLong(4, app.getId());
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
        }
    }

    private void loadDataToMemory() {
        allOffers = (List<Offer>) offerRepository.findAll();
        Map<Long, Offer> offersById = allOffers.stream()
                .collect(Collectors.toMap(Offer::getId, o -> o));

        allApplications = applicationRepository.findAllWithAssociations();
        entityManager.clear();

        for (Application app : allApplications) {
            Offer canonical = offersById.get(app.getOffer().getId());
            if (canonical != null) app.setOffer(canonical);
        }

        budgetApplicationsByKey = allApplications.stream()
                .filter(Application::getIsBudget)
                .collect(Collectors.groupingBy(
                        a -> new ApplicationKey(a.getOffer().getId(), a.getQuotaType())
                ));

        contractApplicationsByOfferId = allApplications.stream()
                .filter(app -> !app.getIsBudget())
                .collect(Collectors.groupingBy(
                        a -> a.getOffer().getId()
                ));

        Map<StudentApplicationKey, List<Application>> studentApplicationsByKey = allApplications.parallelStream()
                .collect(Collectors.groupingBy(
                        a -> new StudentApplicationKey(
                                a.getStudent().getId(),
                                a.getStudent().getRawScore(),
                                a.getQuotaType()
                        ),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    list.sort(Comparator.comparingInt(Application::getPriority));
                                    return list;
                                }
                        )
                ));

        for (List<Application> apps : budgetApplicationsByKey.values()) {
            apps.sort(Comparator.comparingDouble(Application::getScore).reversed());

            for (int i = 1; i < apps.size(); i++)
                apps.get(i).setPreviousInOffer(apps.get(i - 1));
        }

        for (List<Application> apps : contractApplicationsByOfferId.values()) {
            apps.sort(Comparator.comparingDouble(Application::getScore).reversed());

            for (int i = 1; i < apps.size(); i++)
                apps.get(i).setPreviousInOffer(apps.get(i - 1));

        }

        for (List<Application> apps : studentApplicationsByKey.values()) {
            for (Application app : apps)
                app.setStudentSiblingApplications(apps);
        }
    }
    //endregion

    private void analyseData() {
        for (Offer offer : allOffers) {
            for (QuotaType quotaType : QuotaType.values())
                analyseBudgetByQuotaType(offer, quotaType);
        }

        for (Offer offer : allOffers)
            analyseContract(offer);

        for (Offer offer : allOffers) {
            setMinScoreIfFilled(offer, QuotaType.GENERAL);
            setMinScoreIfFilled(offer, QuotaType.QUOTA_1);
            setMinScoreIfFilled(offer, QuotaType.QUOTA_2);

            if (offer.getContractPlaces().equals(offer.getContractPlacesCount()) && offer.getContractPlaces() > 0) {
                List<Application> apps = contractApplicationsByOfferId.getOrDefault(offer.getId(), new ArrayList<>());

                if (offer.getContractPlaces() < apps.size()) {
                    offer.setMinContractScore(apps.get(offer.getContractPlaces() - 1).getScore());
                } else {
                    offer.setMinContractScore(0d);
                }
            } else {
                offer.setMinContractScore(0d);
            }
        }
    }

    private void setMinScoreIfFilled(Offer offer, QuotaType quotaType) {
        int places = getMaxPlaces(offer, quotaType, true);

        if (places > 0 && getCurrentCount(offer, quotaType, true) == places) {
            List<Application> apps = budgetApplicationsByKey.getOrDefault(
                    new ApplicationKey(offer.getId(), quotaType),
                    List.of()
            );

            if (places < apps.size())
                setMinScore(offer, quotaType, apps.get(places - 1).getScore());
            else
                setMinScore(offer, quotaType, 0d);
        } else
            setMinScore(offer, quotaType, 0d);
    }

    private void analyseBudgetByQuotaType(Offer offer, QuotaType quotaType) {
        List<Application> offerApps = budgetApplicationsByKey.getOrDefault(
                new ApplicationKey(offer.getId(), quotaType),
                new ArrayList<>()
        );

        for (Application application : offerApps)
            analyseRecursive(application, quotaType);
    }

    private void analyseContract(Offer offer) {
        List<Application> offerApps = contractApplicationsByOfferId.getOrDefault(
               offer.getId(),
                new ArrayList<>()
        );

        for (Application application : offerApps) {
            analyseRecursive(application, application.getQuotaType());
        }
    }

    private void analyseRecursive(Application application, QuotaType quotaType) {
        Offer offer = application.getOffer();
        boolean isBudget = application.getIsBudget();
        int maxPlaces = getMaxPlaces(application.getOffer(), quotaType, application.getIsBudget());

        Application prevApp = application.getPreviousInOffer();
        if (prevApp != null)
            analyseRecursive(prevApp, prevApp.getQuotaType());

        if (application.getIsChecked())
            return;

        if (getCurrentCount(offer, quotaType, isBudget) >= maxPlaces) {
            application.setIsChecked(true);
            return;
        }

        List<Application> studentApplications = application.getStudentSiblingApplications();

        if (application.getPriority().equals(studentApplications.getFirst().getPriority())) {

            if (getCurrentCount(offer, quotaType, isBudget) >= maxPlaces) {
                application.setIsChecked(true);
                return;
            }

            application.setIsCounted(true);
            incrementCount(offer, quotaType, isBudget);

            for (Application sa : studentApplications)
                checkApplication(sa);

            return;
        }

        boolean hasCounted = false;
        for (Application sa : studentApplications) {
            if (sa.getIsCounted()) {
                hasCounted = true;
                break;
            }
        }

        if (hasCounted) {
            for (Application sa : studentApplications)
                checkApplication(sa);

            return;
        }

        for (Application studentApplication : studentApplications) {
            if (application.getPriority().equals(studentApplication.getPriority())) {
                if (getCurrentCount(offer, quotaType, isBudget) >= maxPlaces) {
                    application.setIsChecked(true);
                    return;
                }

                application.setIsChecked(true);
                application.setIsCounted(true);
                incrementCount(offer, quotaType, isBudget);

                for (Application sa : studentApplications)
                    checkApplication(sa);

                return;
            }

            if (studentApplication.getIsChecked())
                continue;

            analyseRecursive(studentApplication, quotaType);
        }
    }

    private void checkApplication(Application application) {
        application.setIsChecked(true);

        if (application.getPriority() == 1) {
            application.setIsActual(true);
            return;
        }

        List<Application> studentApps = application.getStudentSiblingApplications();

        boolean allPreviousExist = true;

        for (int p = 1; p < application.getPriority(); p++) {
            boolean found = false;
            for (Application sa : studentApps) {
                if (sa.getPriority() == p) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                allPreviousExist = false;
                break;
            }
        }

        application.setIsActual(allPreviousExist);
    }

    private int getMaxPlaces(Offer offer, QuotaType quotaType, boolean isBudget) {
        if (!isBudget)
            return offer.getContractPlaces();

        return switch (quotaType) {
            case GENERAL -> offer.getBudgetPlaces();
            case QUOTA_1 -> offer.getQuota1Places();
            case QUOTA_2 -> offer.getQuota2Places();
        };
    }

    private int getCurrentCount(Offer offer, QuotaType quotaType, boolean isBudget) {
        if (!isBudget) return offer.getContractPlacesCount();

        return switch (quotaType) {
            case GENERAL -> offer.getBudgetPlacesCount();
            case QUOTA_1 -> offer.getQuota1PlacesCount();
            case QUOTA_2 -> offer.getQuota2PlacesCount();
        };
    }

    private void incrementCount(Offer offer, QuotaType quotaType, boolean isBudget) {
        if (!isBudget) {
            offer.setContractPlacesCount(offer.getContractPlacesCount() + 1);
            return;
        }

        switch (quotaType) {
            case GENERAL -> offer.setBudgetPlacesCount(offer.getBudgetPlacesCount() + 1);
            case QUOTA_1 -> offer.setQuota1PlacesCount(offer.getQuota1PlacesCount() + 1);
            case QUOTA_2 -> offer.setQuota2PlacesCount(offer.getQuota2PlacesCount() + 1);
        }
    }

    private void setMinScore(Offer offer, QuotaType quotaType, double score) {
        switch (quotaType) {
            case GENERAL -> offer.setMinBudgetScore(score);
            case QUOTA_1 -> offer.setMinQuota1Score(score);
            case QUOTA_2 -> offer.setMinQuota2Score(score);
        }
    }
}
