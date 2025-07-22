package org.admissio.scraper.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.admissio.scraper.entity.*;
import org.admissio.scraper.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
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
    private Map<Long, List<Application>> applicationsByOffer;
    private Map<String, List<Application>> applicationsByStudentAndScore;

    @Transactional
    public void analyse() {
        setData();

        loadDataToMemory();
        analyseData();
        saveData();
        System.out.println("Application analysed");
    }


    private void analyseData() {
        for ( Offer offer : allOffers) {
            List<Application> offerApps = new ArrayList<>(applicationsByOffer.get(offer.getId()));

            offerApps.sort(Comparator.comparingDouble(Application::getScore).reversed());

            for (Application application : offerApps) {
                analyseRecursive(application);
            }
        }
    }

    public void saveData() {
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

        applicationsByOffer = allApplications.stream()
                .collect(Collectors.groupingBy(a -> a.getOffer().getId()));

        applicationsByStudentAndScore = allApplications.stream()
                .collect(Collectors.groupingBy(a ->
                        a.getStudent().getId() + "_" + a.getRawScore()
                ));
    }

    private void analyseRecursive(Application application) {
        if (application.getIsChecked())
            return;

        if(application.getOffer().getBudgetPlaces() <= application.getOffer().getBudgetPlacesCount()){
            application.setIsChecked(true);
            return;
        }

        Application prevApp = getPrevApplication(application);
        if (prevApp != null)
            analyseRecursive(prevApp);

        if(application.getOffer().getBudgetPlaces() <= application.getOffer().getBudgetPlacesCount()){
            application.setIsChecked(true);
            return;
        }

        List<Application> studentApplications = new ArrayList<>(applicationsByStudentAndScore.get(application.getStudent().getId() + "_" + application.getRawScore()));
        studentApplications.sort(Comparator.comparingInt(Application::getPriority));

        if(application.getPriority().equals(studentApplications.getFirst().getPriority())){
            application.setIsCounted(true);
            application.setIsChecked(true);

            application.getOffer().setBudgetPlacesCount(application.getOffer().getBudgetPlacesCount() + 1);
            return;
        }

        for (Application studentApplication : studentApplications) {
            if (application.getPriority().equals(studentApplication.getPriority())){
                if(studentApplications.stream().anyMatch(Application::getIsCounted))
                    application.setIsChecked(true);

                else {
                    application.setIsCounted(true);
                    application.setIsChecked(true);
                    application.getOffer().setBudgetPlacesCount(application.getOffer().getBudgetPlacesCount() + 1);
                }
                return;
            }


            if(studentApplication.getIsChecked())
                continue;

            analyseRecursive(studentApplication);
        }
    }

    private Application getPrevApplication(Application application) {
        List<Application> applications = new ArrayList<>(applicationsByOffer.get(application.getOffer().getId()));

        applications.sort(Comparator.comparingDouble(Application::getScore).reversed());

        int index = applications.indexOf(application);
        return index > 0 ? applications.get(index - 1) : null;
    }

    private void setData(){
        List<Major> majors = (List<Major>) majorRepository.findAll();
        List<University> universities = (List<University>) universityRepository.findAll();

        int offersCount = majors.size() * universities.size();

        for( Major major : majors){
            for(University university : universities){
                Integer minScore = random.nextInt(100, 130);
                offerRepository.save(new Offer(1L, "Пропозиція 1", major, university, "ФІ", "Денна", random.nextInt(offersCount / 2, offersCount + 1), 0, 0, 0, minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, 0, 1d));
            }
        }

        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 10 * offersCount; i++)
            students.add(new Student("Студент " + (i+1)));

        studentRepository.saveAll(students);

        for (Student student : students) {
            Double score = (double) random.nextInt(100, 200);

            List<Integer> priorities = new ArrayList<>(List.of(1, 2, 3, 4, 5));
            Collections.shuffle(priorities);

            List<Offer> offers = (List<Offer>) offerRepository.findAll();
            Collections.shuffle(offers);

            for (int i = 0; i < priorities.size() - 1; i++){
                applicationRepository.save(new Application(student, offers.get(i), score, score, priorities.get(i), 0, true));
            }
        }
    }
}
