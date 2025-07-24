package org.admissio.scraper.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.admissio.scraper.entity.*;
import org.admissio.scraper.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

    private final Random random = new Random();

    private List<Application> allApplications;
    private List<Offer> allOffers;
    private Map<ApplicationKey, List<Application>> applicationsByKey;
    private Map<StudentApplicationKey, List<Application>> studentApplicationsByKey;

    @Transactional
    public void analyse() {
        setData();

        loadDataToMemory();
        analyseData();
        saveData();
        System.out.println("Application analysed");
    }

    //region Database
    private void saveData() {
        int batchSize = 100;

        saveOffersInBatches(batchSize);
        saveApplicationsInBatches(batchSize);
    }

    private void saveOffersInBatches(int batchSize) {
        for (int i = 0; i < allOffers.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allOffers.size());
            List<Offer> sub = allOffers.subList(i, end);
            offerRepository.saveAll(sub);
            entityManager.flush();
            entityManager.clear();
        }
    }

    private void saveApplicationsInBatches(int batchSize) {
        for (int i = 0; i < allApplications.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allApplications.size());
            List<Application> sub = allApplications.subList(i, end);
            applicationRepository.saveAll(sub);
            entityManager.flush();
            entityManager.clear();
        }
    }

    private void loadDataToMemory(){
        allOffers = (List<Offer>) offerRepository.findAll();
        Map<Long, Offer> offersById = allOffers.stream()
                .collect(Collectors.toMap(Offer::getId, o -> o));

        allApplications = (List<Application>) applicationRepository.findAll();

        for (Application app : allApplications) {
            Long offerId = app.getOffer().getId();
            Offer canonicalOffer = offersById.get(offerId);
            app.setOffer(canonicalOffer);
        }

        applicationsByKey = allApplications.stream()
                .collect(Collectors.groupingBy(a ->
                                new ApplicationKey(
                                        a.getOffer().getId(),
                                        a.getQuotaType()
                                ),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparingDouble(Application::getScore).reversed())
                                        .toList()
                        )
                ));

        studentApplicationsByKey = allApplications.stream()
                .collect(Collectors.groupingBy(a ->
                                new StudentApplicationKey(
                                        a.getStudent().getId(),
                                        a.getRawScore(),
                                        a.getQuotaType()
                                ),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparingDouble(Application::getPriority))
                                        .toList()
                        )
                ));
    }
    //endregion

    private void analyseData() {
        for (Offer offer : allOffers) {
            for (QuotaType quotaType : QuotaType.values()) {
                analyseByQuotaType(offer, quotaType);
            }
        }
    }

    private void analyseByQuotaType(Offer offer, QuotaType quotaType){
        List<Application> offerApps = applicationsByKey.getOrDefault(
                new ApplicationKey(offer.getId(), quotaType),
                new ArrayList<>()
        );

        for (Application application : offerApps) {
            analyseRecursive(application, quotaType);
        }
    }

    private void analyseRecursive(Application application, QuotaType quotaType) {
        Integer maxPlaces = getMaxPlaces(application.getOffer(), quotaType, application.getIsBudget());
        Supplier<Integer> getCount = getGetCount(application.getOffer(), quotaType, application.getIsBudget());
        Consumer<Integer> setCount = getSetCount(application.getOffer(), quotaType, application.getIsBudget());

        if (application.getIsChecked())
            return;

        if(getCount.get() >= maxPlaces){
            application.setIsChecked(true);
            return;
        }

        Application prevApp = getPrevApplication(application, quotaType);
        if (prevApp != null)
            analyseRecursive(prevApp, quotaType);

        if (getCount.get() >= maxPlaces) {
            application.setIsChecked(true);
            return;
        }

        List<Application> studentApplications = studentApplicationsByKey.getOrDefault(
                new StudentApplicationKey(application.getStudent().getId(), application.getRawScore(), quotaType),
                new ArrayList<>()
        );

        if(application.getPriority().equals(studentApplications.getFirst().getPriority())){
            application.setIsCounted(true);

            setCount.accept(getCount.get() + 1);

            studentApplications.forEach(this::checkApplication);

            return;
        }

        for (Application studentApplication : studentApplications) {
            if (studentApplications.stream().anyMatch(Application::getIsCounted) || application.getPriority().equals(studentApplication.getPriority())){
                if(application.getPriority().equals(studentApplication.getPriority())) {
                    application.setIsChecked(true);
                    setCount.accept(getCount.get() + 1);
                }

                studentApplications.forEach(this::checkApplication);

                return;
            }


            if(studentApplication.getIsChecked())
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
                new StudentApplicationKey(application.getStudent().getId(), application.getRawScore(), application.getQuotaType()),
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
            return offer.getBudgetPlaces();

        return switch (quotaType){
            case GENERAL -> offer.getBudgetPlaces();
            case QUOTA_1 -> offer.getQuota1Places();
            case QUOTA_2 -> offer.getQuota2Places();
        };
    }

    private Supplier<Integer> getGetCount(Offer offer, QuotaType quotaType, boolean isBudget) {
        if (!isBudget)
            return offer::getContractPlacesCount;

        return switch (quotaType){
            case GENERAL -> offer::getBudgetPlacesCount;
            case QUOTA_1 -> offer::getQuota1PlacesCount;
            case QUOTA_2 -> offer::getQuota2PlacesCount;
        };
    }

    private Consumer<Integer> getSetCount(Offer offer, QuotaType quotaType, boolean isBudget) {
        if (!isBudget)
            return offer::setContractPlacesCount;

        return switch (quotaType){
            case GENERAL -> offer::setBudgetPlacesCount;
            case QUOTA_1 -> offer::setQuota1PlacesCount;
            case QUOTA_2 -> offer::setQuota2PlacesCount;
        };
    }

    private Application getPrevApplication(Application application, QuotaType quotaType) {
        List<Application> applications = applicationsByKey.getOrDefault(
                new ApplicationKey(application.getOffer().getId(), quotaType),
                new ArrayList<>()
        );

        int index = applications.indexOf(application);
        return index > 0 ? applications.get(index - 1) : null;
    }

    private void setData(){
        List<Major> majors = (List<Major>) majorRepository.findAll();
        List<University> universities = (List<University>) universityRepository.findAll();

        int offersCount = majors.size() * universities.size();

        int count = 1;
        for( Major major : majors){
            for(University university : universities){
                Integer minScore = random.nextInt(100, 130);
                offerRepository.save(new Offer(1L, "Пропозиція" + count, major, university, "ФІ", "Денна",
                        random.nextInt(offersCount / 2, offersCount),
                        random.nextInt(offersCount / 2, offersCount),
                        random.nextInt(offersCount, offersCount * 2) / 10,
                        random.nextInt(offersCount, offersCount * 2) / 10,
                        minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, 0, 1d));
                count++;
            }
        }

        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 10 * offersCount; i++)
            students.add(new Student("Студент " + (i+1)));

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

            for (int i = 0; i < priorities.size() - 2; i++){
                int priority = priorities.get(i);
                if (priority > 5)
                    applicationRepository.save(new Application(student, offers.get(i), score, score, priority, false, quotaType));
                else
                    applicationRepository.save(new Application(student, offers.get(i), score, score, priority, true, quotaType));
            }
        }
    }
}
