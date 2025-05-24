package com.agms.backend.service.impl;

import com.agms.backend.dto.CreateStudentRequest;
import com.agms.backend.model.users.Student;
import com.agms.backend.model.users.User;
import com.agms.backend.model.users.Role;
import com.agms.backend.model.AdvisorList;
import com.agms.backend.repository.StudentRepository;
import com.agms.backend.repository.UserRepository;
import com.agms.backend.repository.AdvisorListRepository;
import com.agms.backend.exception.ResourceNotFoundException;
import com.agms.backend.exception.EmailAlreadyExistsException;
import com.agms.backend.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdvisorListRepository advisorListRepository;

    @Autowired
    public StudentServiceImpl(
            StudentRepository studentRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AdvisorListRepository advisorListRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.advisorListRepository = advisorListRepository;
    }

    @Override
    @Transactional
    public Student createStudent(CreateStudentRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        if (studentRepository.existsByStudentNumber(request.getStudentNumber())) {
            throw new EmailAlreadyExistsException("Student number already exists");
        }

        Student student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .studentNumber(request.getStudentNumber()) // This is now the primary key
                .build();

        return studentRepository.save(student);
    }

    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Optional<Student> getStudentByStudentNumber(String studentNumber) {
        return studentRepository.findByStudentNumber(studentNumber);
    }

    @Override
    public Student updateStudent(String studentNumber, Student studentDetails) {
        Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with number: " + studentNumber));

        // Update allowed fields
        if (studentDetails.getFirstName() != null) {
            student.setFirstName(studentDetails.getFirstName());
        }
        if (studentDetails.getLastName() != null) {
            student.setLastName(studentDetails.getLastName());
        }
        if (studentDetails.getEmail() != null) {
            student.setEmail(studentDetails.getEmail());
        }
        // Note: Graduation status is now handled through submissions, not directly on student

        return studentRepository.save(student);
    }

    @Override
    public void deleteStudent(String studentNumber) {
        Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with number: " + studentNumber));
        studentRepository.delete(student);
    }

    @Override
    public void updateGraduationRequestStatus(String studentNumber, String status) {
        // Note: Graduation status is now handled through submissions, not directly on student
        // This method should create or update a submission with the given status
        throw new UnsupportedOperationException("Graduation status is now handled through submissions. Use submission service instead.");
    }

    @Override
    @Transactional
    public void assignAdvisor(String studentNumber, String advisorListId) {
        Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with number: " + studentNumber));

        AdvisorList advisorList = advisorListRepository.findById(advisorListId)
                .orElseThrow(() -> new ResourceNotFoundException("Advisor list not found with ID: " + advisorListId));

        student.setAdvisorList(advisorList);
        studentRepository.save(student);
    }

    @Override
    @Transactional
    public void removeAdvisor(String studentNumber) {
        Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with number: " + studentNumber));

        student.setAdvisorList(null);
        studentRepository.save(student);
    }
}