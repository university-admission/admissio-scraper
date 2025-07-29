package org.admissio.scraper.service;

import jakarta.annotation.PostConstruct;
import org.admissio.scraper.dto.offer.OfferDetailsDto;
import org.admissio.scraper.dto.offer.SubjectDetailsDto;
import org.admissio.scraper.entity.Major;
import org.admissio.scraper.repository.MajorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MajorService {
    private final MajorRepository majorRepository;
    public static List<Major> majorsCache;
    private final String[] supportedMajors = {
            "A2", "A3", "A4.04", "A4.05", "A4.06", "A4.07",
            "A4.08",  "A4.09", "A4.10", "A4.15", "A4.16",
            "A5", "A6.02", "A6.03", "A6.04", "A6.05",
            "E3", "E4", "E5", "E6", "E7", "E8",
            "G1", "G2", "G3", "G4", "G5", "G6", "G7",
            "G8", "G9", "G10", "G11", "G12", "G13", "G14", "G15", "G16", "G19",
            "H1", "H2", "H3", "H4", "H5", "H7", "J6",
            "J7", "J8", "K1", "K2", "K5", "K6", "k7", "K8", "K10"
    };

    MajorService(MajorRepository majorRepository) {
        this.majorRepository = majorRepository;

    }

    @PostConstruct
    public void init() {
        this.majorsCache = (List<Major>) majorRepository.findAll();
    }

    public Major addMajor(OfferDetailsDto dto) {
        Major major = new Major();
        major.setMajorCode(dto.getDetailedMajorCode() != null ? dto.getDetailedMajorCode() : dto.getMajorCode());
        major.setMajorName(dto.getDetailedName() != null ? dto.getDetailedName() : dto.getName());
        major.setCompetitionCoef(0d); // Default value
        setSubjectsCoef(dto, major);
        if (checkSupportedMajor(major.getMajorCode())) {
            major.setMajorCoef(1.02);
        } else {
            major.setMajorCoef(1d);
        }

        //majorRepository.save(major);
        majorsCache.add(major);
        return major;
    }

    public Optional<Major> getMajor(String majorCode) {
        for (Major major : majorsCache) {
            if (major.getMajorCode().equalsIgnoreCase(majorCode)) {
                return Optional.of(major);
            }
        }
        return Optional.empty();
    }

    private void setSubjectsCoef(OfferDetailsDto dto, Major major) {
        if (dto.getSubjectDetailsMap() != null) {
            for (SubjectDetailsDto subject : dto.getSubjectDetailsMap().values()) {
                switch (subject.getSubjectName()) {
                    case "Українська мова":
                        major.setUkLanguageCoef(subject.getSubjectCoef());
                        break;
                    case "Математика":
                        major.setMathCoef(subject.getSubjectCoef());
                        break;
                    case "Історія України":
                        major.setHistoryCoef(subject.getSubjectCoef());
                        break;
                    case "Українська література":
                        major.setUkLiteratureCoef(subject.getSubjectCoef());
                        break;
                    case "Іноземна мова":
                        major.setForeignLangCoef(subject.getSubjectCoef());
                        break;
                    case "Біологія":
                        major.setBiologyCoef(subject.getSubjectCoef());
                        break;
                    case "Географія":
                        major.setGeographyCoef(subject.getSubjectCoef());
                        break;
                    case "Фізика":
                        major.setPhysicsCoef(subject.getSubjectCoef());
                        break;
                    case "Хімія":
                        major.setChemistryCoef(subject.getSubjectCoef());
                        break;
                    case "Творчий конкурс":
                        major.setCompetitionCoef(subject.getSubjectCoef());
                        break;
                    case "Бал за успішне закінчення підготовчих курсів закладу освіти":
                        break;
                    case "Мотиваційний лист":
                        break;
                    default:
                        // Log or handle subjects that are not mapped to entity
                        System.err.println("Unhandled subject: " + subject.getSubjectName() + " with coef: " + subject.getSubjectCoef());
                }
            }
        }
    }

    private boolean checkSupportedMajor(String majorCode) {
        for (String supportedMajor : supportedMajors) {
            if (majorCode.equalsIgnoreCase(supportedMajor)) {
                return true;
            }
        }
        return false;
    }
}
