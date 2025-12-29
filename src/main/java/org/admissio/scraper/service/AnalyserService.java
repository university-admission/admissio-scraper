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
import java.util.function.Consumer;
import java.util.function.Supplier;
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
    StudentRepository studentRepository;
    @NonNull
    UniversityRepository universityRepository;
    @NonNull
    MajorRepository majorRepository;
    @NonNull
    EntityManager entityManager;
    @NonNull
    JdbcTemplate jdbcTemplate;

    private final Random random = new Random();

    private List<Application> allApplications;
    private List<Offer> allOffers;
    private Map<ApplicationKey, List<Application>> budgetApplicationsByKey;
    private Map<Long, List<Application>> contractApplicationsByOfferId;
    private Map<StudentApplicationKey, List<Application>> studentApplicationsByKey;

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
                public void setValues(PreparedStatement ps, int i) throws SQLException {
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

        studentApplicationsByKey = allApplications.parallelStream()
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
        Supplier<Integer> getCount = getGetCount(offer, quotaType, true);
        Consumer<Double> setScore = getGetMinScore(offer, quotaType);

        if (getCount.get().equals(places) && places > 0) {
            List<Application> apps = budgetApplicationsByKey.getOrDefault(
                    new ApplicationKey(offer.getId(), quotaType),
                    List.of()
            );

            if (places < apps.size()) {
                setScore.accept(apps.get(places - 1).getScore());
            } else {
                setScore.accept(0d);
            }
        } else {
            setScore.accept(0d);
        }
    }

    private void analyseBudgetByQuotaType(Offer offer, QuotaType quotaType) {
        List<Application> offerApps = budgetApplicationsByKey.getOrDefault(
                new ApplicationKey(offer.getId(), quotaType),
                new ArrayList<>()
        );

        for (Application application : offerApps) {
            analyseRecursive(application, quotaType);
        }
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
        Integer maxPlaces = getMaxPlaces(application.getOffer(), quotaType, application.getIsBudget());
        Supplier<Integer> getCount = getGetCount(application.getOffer(), quotaType, application.getIsBudget());
        Consumer<Integer> setCount = getSetCount(application.getOffer(), quotaType, application.getIsBudget());

        Application prevApp = application.getIsBudget() ? getPrevApplication(application, quotaType) : getPrevApplication(application);
        if (prevApp != null)
            analyseRecursive(prevApp, prevApp.getQuotaType());

        if (application.getIsChecked())
            return;

        if (getCount.get() >= maxPlaces) {
            application.setIsChecked(true);
            return;
        }

        if (getCount.get() >= maxPlaces) {
            application.setIsChecked(true);
            return;
        }

        List<Application> studentApplications = studentApplicationsByKey.getOrDefault(
                new StudentApplicationKey(application.getStudent().getId(), application.getStudent().getRawScore(), quotaType),
                new ArrayList<>()
        );

        if (application.getPriority().equals(studentApplications.getFirst().getPriority())) {

            if (getCount.get() >= maxPlaces) {
                application.setIsChecked(true);
                return;
            }

            application.setIsCounted(true);
            setCount.accept(getCount.get() + 1);
            studentApplications.forEach(this::checkApplication);

            return;
        }

        for (Application studentApplication : studentApplications) {
            if (studentApplications.stream().anyMatch(Application::getIsCounted)) {
                studentApplications.forEach(this::checkApplication);
                return;
            }

            if (application.getPriority().equals(studentApplication.getPriority())) {
                if (getCount.get() >= maxPlaces) {
                    application.setIsChecked(true);
                    return;
                }

                application.setIsChecked(true);
                application.setIsCounted(true);
                setCount.accept(getCount.get() + 1);
                studentApplications.forEach(this::checkApplication);
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

        List<Application> studentApps = studentApplicationsByKey.getOrDefault(
                new StudentApplicationKey(application.getStudent().getId(), application.getStudent().getRawScore(), application.getQuotaType()),
                Collections.emptyList()
        );

        Set<Integer> existingPriorities = studentApps.stream()
                .map(Application::getPriority)
                .collect(Collectors.toSet());

        boolean allPreviousExist = true;
        for (int p = 1; p < application.getPriority(); p++) {
            if (!existingPriorities.contains(p)) {
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

    private Supplier<Integer> getGetCount(Offer offer, QuotaType quotaType, boolean isBudget) {
        if (!isBudget)
            return offer::getContractPlacesCount;

        return switch (quotaType) {
            case GENERAL -> offer::getBudgetPlacesCount;
            case QUOTA_1 -> offer::getQuota1PlacesCount;
            case QUOTA_2 -> offer::getQuota2PlacesCount;
        };
    }

    private Consumer<Integer> getSetCount(Offer offer, QuotaType quotaType, boolean isBudget) {
        if (!isBudget)
            return offer::setContractPlacesCount;

        return switch (quotaType) {
            case GENERAL -> offer::setBudgetPlacesCount;
            case QUOTA_1 -> offer::setQuota1PlacesCount;
            case QUOTA_2 -> offer::setQuota2PlacesCount;
        };
    }

    private Consumer<Double> getGetMinScore(Offer offer, QuotaType quotaType) {
        return switch (quotaType) {
            case GENERAL -> offer::setMinBudgetScore;
            case QUOTA_1 -> offer::setMinQuota1Score;
            case QUOTA_2 -> offer::setMinQuota2Score;
        };
    }

    private Application getPrevApplication(Application application, QuotaType quotaType) {
        List<Application> applications = budgetApplicationsByKey.getOrDefault(
                new ApplicationKey(application.getOffer().getId(), quotaType),
                new ArrayList<>()
        );

        int index = applications.indexOf(application);
        return index > 0 ? applications.get(index - 1) : null;
    }

    private Application getPrevApplication(Application application) {
        List<Application> applications = contractApplicationsByOfferId.getOrDefault(
                application.getOffer().getId(),
                new ArrayList<>()
        );

        int index = applications.indexOf(application);
        return index > 0 ? applications.get(index - 1) : null;
    }

    public void setData() {
        List<Major> majors = (List<Major>) majorRepository.findAll();
        List<University> universities = (List<University>) universityRepository.findAll();

        int offersCount = majors.size() * universities.size();

        int count = 1;
        for (Major major : majors) {
            for (University university : universities) {
                Integer minScore = random.nextInt(100, 130);
                List<EducationForm> educationForms = new ArrayList<>(List.of(EducationForm.values()));
                offerRepository.save(new Offer(1L, "Пропозиція " + count, major, university, "ФІ", "1", 1,
                        educationForms.get(random.nextInt(educationForms.size())),
                        random.nextInt(1, offersCount),
                        random.nextInt(1, offersCount),
                        Math.max(1, random.nextInt(offersCount) / 10),
                        Math.max(1, random.nextInt(offersCount) / 10),
                        minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, 0, 1d));
                count++;
            }
        }

        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 10 * offersCount; i++)
            students.add(new Student("Студент " + (i + 1), (double) random.nextInt(100, 200)));

        studentRepository.saveAll(students);

        for (Student student : students) {
            List<QuotaType> quotaTypes = new ArrayList<>(List.of(QuotaType.values()));
            Collections.shuffle(quotaTypes);
            QuotaType quotaType = quotaTypes.getFirst();

            Double score = (double) random.nextInt(100, 200);

            List<Integer> priorities = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
            Collections.shuffle(priorities);

            List<Offer> offers = (List<Offer>) offerRepository.findAll();
            Collections.shuffle(offers);

            for (int i = 0; i < priorities.size() - 2; i++) {
                int priority = priorities.get(i);
                if (priority > 5) {
                    applicationRepository.save(new Application(student, offers.get(i), score, priority, false, quotaType));
                    switch (quotaType){
                        case GENERAL -> offers.get(i).setBudgetApplications(offers.get(i).getBudgetApplications() + 1);
                        case QUOTA_1 -> offers.get(i).setQuota1Applications(offers.get(i).getQuota1Applications() + 1);
                        case QUOTA_2 -> offers.get(i).setQuota2Applications(offers.get(i).getQuota2Applications() + 1);
                        default -> {
                            return;
                        }
                    }
                }
                else {
                    applicationRepository.save(new Application(student, offers.get(i), score, priority, true, quotaType));
                    offers.get(i).setContractApplications(offers.get(i).getContractApplications() + 1);
                }
            }
        }
    }
}
