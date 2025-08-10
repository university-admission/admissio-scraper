package org.admissio.scraper.service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.admissio.scraper.entity.Student;
import org.admissio.scraper.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class StudentService {
    @NonNull
    private StudentRepository studentRepository;
    public static Map<String, Student> studentsCache = new HashMap<>();

    @PostConstruct
    public void init(){
        for (Student student : studentRepository.findAll()) {
            String key = generateStudentKey(student.getFullName(), student.getRawScore());
            studentsCache.put(key, student);
        }
    }

    public Student getOrCreateStudent(String fullName, Double rawScoreSum){
        String key = generateStudentKey(fullName, rawScoreSum);

        if (studentsCache.containsKey(key)){
            return studentsCache.get(key);
        }

        Student newStudent = new Student();
        newStudent.setFullName(fullName);
        newStudent.setRawScore(rawScoreSum);

        studentsCache.put(key, newStudent);
        studentRepository.save(newStudent);
        return newStudent;
    }

    private String generateStudentKey(String fullName, Double rawScore) {
        return fullName.toLowerCase() + "_" + rawScore;
    }

}
