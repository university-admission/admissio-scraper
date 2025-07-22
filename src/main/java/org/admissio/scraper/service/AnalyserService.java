package org.admissio.scraper.service;

import lombok.AllArgsConstructor;
import org.admissio.scraper.entity.*;
import org.admissio.scraper.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class AnalyserService {
    ApplicationRepository applicationRepository;
    OfferRepository offerRepository;
    StudentRepository studentRepository;
    UniversityRepository universityRepository;
    MajorRepository majorRepository;

    private final Random random = new Random();

    public void analyse() {
        System.out.println("Analyser service started\nStart loading data");
        setData();
        System.out.println("Data loaded successful\nStart analysing data");
        analyseData();
        System.out.println("Data analysed successful");
    }

    private void analyseData() {
        for ( Offer offer : offerRepository.findAll()) {
            for (Application application : applicationRepository.findAllByOfferOrderByScoreDesc(offer)) {
                analyseRecursive(application);
            }
        }
    }

    private void analyseRecursive(Application application) {
        if (application.getIsChecked())
            return;

        if(application.getOffer().getBudgetPlaces() <= application.getOffer().getBudgetPlacesCount()){
            application.setIsChecked(true);
            applicationRepository.save(application);
            return;
        }

        Application prevApp = getPrevApplication(application);
        if (prevApp != null)
            analyseRecursive(prevApp);

        List<Application> studentApplications = applicationRepository.findAllByStudentAndRawScoreOrderByPriority(application.getStudent(), application.getRawScore());

        if(application.getPriority().equals(studentApplications.getFirst().getPriority())){
            application.setIsCounted(true);
            application.setIsChecked(true);

            application.getOffer().setBudgetPlacesCount(application.getOffer().getBudgetPlacesCount() + 1);
            applicationRepository.save(application);
            offerRepository.save(application.getOffer());
            return;
        }

        for (Application studentApplication : studentApplications) {
            if (application.getPriority().equals(studentApplication.getPriority())){
                if(studentApplications.stream().anyMatch(Application::getIsCounted)) {
                    application.setIsChecked(true);
                    applicationRepository.save(application);
                }
                else {
                    application.setIsCounted(true);
                    application.setIsChecked(true);
                    application.getOffer().setBudgetPlacesCount(application.getOffer().getBudgetPlacesCount() + 1);
                    offerRepository.save(application.getOffer());
                    applicationRepository.save(application);
                }
                return;
            }


            if(studentApplication.getIsChecked())
                continue;

            analyseRecursive(studentApplication);
        }
    }

    private Application getPrevApplication(Application application) {
        Offer offer = offerRepository.findById(application.getOffer().getId()).get();
        List<Application> applications = applicationRepository.findAllByOfferOrderByScoreDesc(offer);
        int index = applications.indexOf(application);
        if (index - 1 < 0)
            return null;
        else
            return applications.get(index - 1);
    }

    private void setData(){
        List<Major> majors = (List<Major>) majorRepository.findAll();
        List<University> universities = (List<University>) universityRepository.findAll();

        int offersCount = majors.size() * universities.size();

        for( Major major : majors){
            for(University university : universities){
                Integer minScore = random.nextInt(100, 130);
                offerRepository.save(new Offer(1L, "Пропозиція 1", major, university, "ФІ", "Денна", random.nextInt(offersCount / 2, offersCount * 2), 0, 0, 0, minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, minScore, 0, 1d));
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

            for (int i = 0; i < priorities.size(); i++){
                applicationRepository.save(new Application(student, offers.get(i), score, score, priorities.get(i), 0, true));
            }
        }
    }
}
