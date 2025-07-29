package org.admissio.scraper.service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.admissio.scraper.entity.Student;
import org.admissio.scraper.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class StudentService {
    @NonNull
    private StudentRepository studentRepository;
    public static List<Student> studentsCache;

    @PostConstruct
    public void init(){
        this.studentsCache = (List<Student>) studentRepository.findAll();
    }

    public Student getOrCreateStudent(String fullName, Double rawScoreSum){
        for (Student student : studentsCache){
            if (student.getFullName().equalsIgnoreCase(fullName) &&  student.getRawScore().equals(rawScoreSum)){
                return student;
            }
        }
        Student newStudent = new Student();
        newStudent.setFullName(fullName);
        newStudent.setRawScore(rawScoreSum);
        studentsCache.add(newStudent);
        return newStudent;
    }

}
