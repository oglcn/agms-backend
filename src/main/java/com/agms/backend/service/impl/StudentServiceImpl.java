package com.agms.backend.service.impl;

import com.agms.backend.dto.CreateStudentRequest;
import com.agms.backend.dto.StudentProfileResponse;
import com.agms.backend.dto.StudentResponse;
import com.agms.backend.model.users.Student;
import com.agms.backend.model.users.User;
import com.agms.backend.model.users.Role;
import com.agms.backend.model.users.Advisor;
import com.agms.backend.model.users.DepartmentSecretary;
import com.agms.backend.model.AdvisorList;
import com.agms.backend.repository.StudentRepository;
import com.agms.backend.repository.UserRepository;
import com.agms.backend.repository.AdvisorListRepository;
import com.agms.backend.exception.ResourceNotFoundException;
import com.agms.backend.exception.EmailAlreadyExistsException;
import com.agms.backend.service.StudentService;
import com.agms.backend.service.UbysService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

@Service
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdvisorListRepository advisorListRepository;
    private final UbysService ubysService;

    @Autowired
    public StudentServiceImpl(
            StudentRepository studentRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AdvisorListRepository advisorListRepository,
            UbysService ubysService) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.advisorListRepository = advisorListRepository;
        this.ubysService = ubysService;
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
    public Optional<StudentResponse> getStudentResponseByStudentNumber(String studentNumber) {
        return studentRepository.findByStudentNumber(studentNumber)
                .map(this::convertToStudentResponse);
    }

    private StudentResponse convertToStudentResponse(Student student) {
        // Get enhanced student data with academic info if available
        Student enhancedStudent;
        try {
            enhancedStudent = ubysService.getStudentWithTransientAttributes(student.getStudentNumber());
        } catch (ResourceNotFoundException e) {
            // If not found in ubys.json, use database student
            enhancedStudent = student;
        }
        
        // Convert advisor to safe DTO
        StudentResponse.AdvisorInfo advisorInfo = null;
        if (student.getAdvisor() != null) {
            Advisor advisor = student.getAdvisor();
            advisorInfo = StudentResponse.AdvisorInfo.builder()
                    .empId(advisor.getEmpId())
                    .firstName(advisor.getFirstName())
                    .lastName(advisor.getLastName())
                    .email(advisor.getEmail())
                    .build();
        }
        
        return StudentResponse.builder()
                .studentNumber(student.getStudentNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .gpa(enhancedStudent.getGpa())
                .totalCredit(enhancedStudent.getTotalCredit())
                .semester(enhancedStudent.getSemester())
                .advisor(advisorInfo)
                .build();
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
        // This method is no longer needed since students don't have direct access to advisor lists.
        // Submissions will be the bridge between students and advisors.
        throw new UnsupportedOperationException("Students no longer have direct access to advisor lists. Use submission service to create graduation requests.");
    }

    @Override
    @Transactional
    public void removeAdvisor(String studentNumber) {
        // This method is no longer needed since students don't have direct access to advisor lists.
        // Submissions will be the bridge between students and advisors.
        throw new UnsupportedOperationException("Students no longer have direct access to advisor lists. Use submission service to manage graduation requests.");
    }

    @Override
    public StudentProfileResponse getStudentProfileByEmail(String email) {
        // Get student from database
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with email: " + email));

        // Get enhanced student data from ubys.json (includes academic info)
        Student enhancedStudent;
        try {
            enhancedStudent = ubysService.getStudentWithTransientAttributes(student.getStudentNumber());
        } catch (ResourceNotFoundException e) {
            // If not found in ubys.json, use database student
            enhancedStudent = student;
        }

        // Get department information from advisor relationship
        String department = null;
        String faculty = null;
        StudentProfileResponse.AdvisorInfo advisorInfo = null;

        if (student.getAdvisor() != null) {
            Advisor advisor = student.getAdvisor();
            advisorInfo = StudentProfileResponse.AdvisorInfo.builder()
                    .empId(advisor.getEmpId())
                    .firstName(advisor.getFirstName())
                    .lastName(advisor.getLastName())
                    .email(advisor.getEmail())
                    .build();

            // Get department from advisor's department secretary
            if (advisor.getDepartmentSecretary() != null) {
                DepartmentSecretary departmentSecretary = advisor.getDepartmentSecretary();
                
                // Get department and faculty info from ubys.json
                try {
                    Map<String, Object> allData = ubysService.getAllData();
                    Map<String, Object> departmentSecretariesSection = (Map<String, Object>) allData.get("departmentSecretaries");
                    
                    if (departmentSecretariesSection != null) {
                        Map<String, Object> deptSecData = (Map<String, Object>) departmentSecretariesSection.get(departmentSecretary.getEmpId());
                        if (deptSecData != null) {
                            department = (String) deptSecData.get("department");
                            
                            // Get faculty from dean officer
                            String deanOfficerId = (String) deptSecData.get("deanOfficerId");
                            if (deanOfficerId != null) {
                                Map<String, Object> deanOfficersSection = (Map<String, Object>) allData.get("deanOfficers");
                                if (deanOfficersSection != null) {
                                    Map<String, Object> deanOfficerData = (Map<String, Object>) deanOfficersSection.get(deanOfficerId);
                                    if (deanOfficerData != null) {
                                        faculty = (String) deanOfficerData.get("faculty");
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Log error but continue - we'll just not have department/faculty info
                    System.err.println("Error getting department/faculty info: " + e.getMessage());
                }
            }
        }

        return StudentProfileResponse.builder()
                .studentNumber(student.getStudentNumber())
                .email(student.getEmail())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .role(student.getRole())
                .department(department)
                .faculty(faculty)
                .advisor(advisorInfo)
                .gpa(enhancedStudent.getGpa() > 0 ? enhancedStudent.getGpa() : null)
                .totalCredits(enhancedStudent.getTotalCredit() > 0 ? enhancedStudent.getTotalCredit() : null)
                .semester(enhancedStudent.getSemester() > 0 ? enhancedStudent.getSemester() : null)
                .build();
    }
}