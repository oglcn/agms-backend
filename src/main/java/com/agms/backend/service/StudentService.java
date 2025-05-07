package com.agms.backend.service;

import com.agms.backend.entity.Student;
import com.agms.backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    @Autowired
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Student createStudent(Student student) {
        // Add any validation or business logic before saving
        return studentRepository.save(student);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> getStudentById(String id) {
        return studentRepository.findById(id);
    }

    public Student updateStudent(String id, Student studentDetails) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id)); // Or a custom exception

        student.setGraduationRequestStatus(studentDetails.getGraduationRequestStatus());

        return studentRepository.save(student);
    }

    public void deleteStudent(String id) {
        if (!studentRepository.existsById(id)) {
            throw new RuntimeException("Student not found with id: " + id); // Or a custom exception
        }
        studentRepository.deleteById(id);
    }
}
