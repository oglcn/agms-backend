package com.agms.backend.service.impl;

import com.agms.backend.dto.CreateStudentRequest;
import com.agms.backend.entity.Student;
import com.agms.backend.entity.User;
import com.agms.backend.entity.Role;
import com.agms.backend.entity.AdvisorList;
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

        if (studentRepository.existsByStudentId(request.getStudentId())) {
            throw new EmailAlreadyExistsException("Student ID already exists");
        }

        Student student = Student.builder()
                .id(UUID.randomUUID().toString())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .studentId(request.getStudentId())
                .graduationStatus(
                        request.getGraduationRequestStatus() != null ? request.getGraduationRequestStatus().toString()
                                : "NOT_REQUESTED")
                .build();

        return studentRepository.save(student);
    }

    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Optional<Student> getStudentByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    @Override
    public Student updateStudent(String studentId, Student studentDetails) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

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
        if (studentDetails.getGraduationStatus() != null) {
            student.setGraduationStatus(studentDetails.getGraduationStatus());
        }

        return studentRepository.save(student);
    }

    @Override
    public void deleteStudent(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));
        studentRepository.delete(student);
    }

    @Override
    public void updateGraduationRequestStatus(String studentId, String status) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));
        student.setGraduationStatus(status);
        studentRepository.save(student);
    }

    @Override
    @Transactional
    public void assignAdvisor(String studentId, String advisorListId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        AdvisorList advisorList = advisorListRepository.findById(advisorListId)
                .orElseThrow(() -> new ResourceNotFoundException("Advisor list not found with ID: " + advisorListId));

        student.setAdvisorList(advisorList);
        studentRepository.save(student);
    }

    @Override
    @Transactional
    public void removeAdvisor(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        student.setAdvisorList(null);
        studentRepository.save(student);
    }
}