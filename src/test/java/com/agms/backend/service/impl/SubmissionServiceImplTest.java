package com.agms.backend.service.impl;

import com.agms.backend.dto.CreateSubmissionRequest;
import com.agms.backend.dto.SubmissionResponse;
import com.agms.backend.exception.ResourceNotFoundException;
import com.agms.backend.model.AdvisorList;
import com.agms.backend.model.Submission;
import com.agms.backend.model.SubmissionStatus;
import com.agms.backend.model.users.Advisor;
import com.agms.backend.model.users.Student;
import com.agms.backend.repository.AdvisorListRepository;
import com.agms.backend.repository.AdvisorRepository;
import com.agms.backend.repository.StudentRepository;
import com.agms.backend.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AdvisorRepository advisorRepository;

    @Mock
    private AdvisorListRepository advisorListRepository;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private Student testStudent;
    private Advisor testAdvisor;
    private AdvisorList testAdvisorList;
    private Submission testSubmission;
    private CreateSubmissionRequest testRequest;

    @BeforeEach
    void setUp() {
        // Create test student
        testStudent = Student.builder()
                .studentNumber("S101")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@student.edu")
                .build();

        // Create test advisor
        testAdvisor = Advisor.builder()
                .empId("ADV101")
                .firstName("Prof")
                .lastName("Smith")
                .email("prof.smith@edu")
                .build();

        // Create test advisor list
        testAdvisorList = AdvisorList.builder()
                .advisorListId("AL_001")
                .creationDate(LocalDate.now())
                .advisor(testAdvisor)
                .build();

        // Set relationships
        testStudent.setAdvisor(testAdvisor);
        testAdvisor.setAdvisorList(testAdvisorList);

        // Create test submission
        testSubmission = Submission.builder()
                .submissionId("SUB_123456789")
                .submissionDate(LocalDate.now())
                .content("Graduation request for John Doe")
                .status(SubmissionStatus.PENDING)
                .student(testStudent)
                .advisorList(testAdvisorList)
                .build();

        // Create test request
        testRequest = CreateSubmissionRequest.builder()
                .studentNumber("S101")
                .content("Graduation request for John Doe")
                .build();
    }

    @Test
    void createGraduationSubmission_Success() {
        // Arrange
        when(studentRepository.findByStudentNumber("S101")).thenReturn(Optional.of(testStudent));
        when(submissionRepository.hasActivePendingSubmission("S101")).thenReturn(false);
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);

        // Act
        SubmissionResponse response = submissionService.createGraduationSubmission(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals("SUB_123456789", response.getSubmissionId());
        assertEquals("S101", response.getStudentNumber());
        assertEquals("John Doe", response.getStudentName());
        assertEquals(SubmissionStatus.PENDING, response.getStatus());
        assertEquals("AL_001", response.getAdvisorListId());
        assertEquals("Graduation request for John Doe", response.getContent());

        verify(submissionRepository).save(any(Submission.class));
    }

    @Test
    void createGraduationSubmission_StudentNotFound() {
        // Arrange
        when(studentRepository.findByStudentNumber("S101")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            submissionService.createGraduationSubmission(testRequest);
        });

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void createGraduationSubmission_StudentHasPendingSubmission() {
        // Arrange
        when(studentRepository.findByStudentNumber("S101")).thenReturn(Optional.of(testStudent));
        when(submissionRepository.hasActivePendingSubmission("S101")).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            submissionService.createGraduationSubmission(testRequest);
        });

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void createGraduationSubmission_StudentHasNoAdvisor() {
        // Arrange
        testStudent.setAdvisor(null);
        when(studentRepository.findByStudentNumber("S101")).thenReturn(Optional.of(testStudent));
        when(submissionRepository.hasActivePendingSubmission("S101")).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            submissionService.createGraduationSubmission(testRequest);
        });

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void createGraduationSubmission_AdvisorHasNoList() {
        // Arrange
        testAdvisor.setAdvisorList(null);
        when(studentRepository.findByStudentNumber("S101")).thenReturn(Optional.of(testStudent));
        when(submissionRepository.hasActivePendingSubmission("S101")).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            submissionService.createGraduationSubmission(testRequest);
        });

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void getSubmissionsByStudent_Success() {
        // Arrange
        List<Submission> submissions = Arrays.asList(testSubmission);
        when(submissionRepository.findByStudentNumber("S101")).thenReturn(submissions);

        // Act
        List<SubmissionResponse> responses = submissionService.getSubmissionsByStudent("S101");

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("SUB_123456789", responses.get(0).getSubmissionId());
        assertEquals("S101", responses.get(0).getStudentNumber());
    }

    @Test
    void getSubmissionsByAdvisor_Success() {
        // Arrange
        List<Submission> submissions = Arrays.asList(testSubmission);
        when(advisorRepository.findByEmpId("ADV101")).thenReturn(Optional.of(testAdvisor));
        when(submissionRepository.findByAdvisorListId("AL_001")).thenReturn(submissions);

        // Act
        List<SubmissionResponse> responses = submissionService.getSubmissionsByAdvisor("ADV101");

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("SUB_123456789", responses.get(0).getSubmissionId());
        assertEquals("AL_001", responses.get(0).getAdvisorListId());
    }

    @Test
    void getSubmissionsByAdvisor_AdvisorNotFound() {
        // Arrange
        when(advisorRepository.findByEmpId("ADV101")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            submissionService.getSubmissionsByAdvisor("ADV101");
        });
    }

    @Test
    void updateSubmissionStatus_Success() {
        // Arrange
        when(submissionRepository.findById("SUB_123456789")).thenReturn(Optional.of(testSubmission));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);

        // Act
        SubmissionResponse response = submissionService.updateSubmissionStatus("SUB_123456789", SubmissionStatus.APPROVED);

        // Assert
        assertNotNull(response);
        assertEquals(SubmissionStatus.APPROVED, testSubmission.getStatus());
        verify(submissionRepository).save(testSubmission);
    }

    @Test
    void updateSubmissionStatus_SubmissionNotFound() {
        // Arrange
        when(submissionRepository.findById("SUB_123456789")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            submissionService.updateSubmissionStatus("SUB_123456789", SubmissionStatus.APPROVED);
        });

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void hasActivePendingSubmission_ReturnsTrue() {
        // Arrange
        when(submissionRepository.hasActivePendingSubmission("S101")).thenReturn(true);

        // Act
        boolean result = submissionService.hasActivePendingSubmission("S101");

        // Assert
        assertTrue(result);
    }

    @Test
    void hasActivePendingSubmission_ReturnsFalse() {
        // Arrange
        when(submissionRepository.hasActivePendingSubmission("S101")).thenReturn(false);

        // Act
        boolean result = submissionService.hasActivePendingSubmission("S101");

        // Assert
        assertFalse(result);
    }

    @Test
    void getLatestSubmissionByStudent_Success() {
        // Arrange
        List<Submission> submissions = Arrays.asList(testSubmission);
        when(submissionRepository.findByStudentNumberOrderBySubmissionDateDesc("S101")).thenReturn(submissions);

        // Act
        Optional<SubmissionResponse> response = submissionService.getLatestSubmissionByStudent("S101");

        // Assert
        assertTrue(response.isPresent());
        assertEquals("SUB_123456789", response.get().getSubmissionId());
    }

    @Test
    void getLatestSubmissionByStudent_NoSubmissions() {
        // Arrange
        when(submissionRepository.findByStudentNumberOrderBySubmissionDateDesc("S101")).thenReturn(Arrays.asList());

        // Act
        Optional<SubmissionResponse> response = submissionService.getLatestSubmissionByStudent("S101");

        // Assert
        assertTrue(response.isEmpty());
    }

    @Test
    void deleteSubmission_Success() {
        // Arrange
        when(submissionRepository.existsById("SUB_123456789")).thenReturn(true);

        // Act
        submissionService.deleteSubmission("SUB_123456789");

        // Assert
        verify(submissionRepository).deleteById("SUB_123456789");
    }

    @Test
    void deleteSubmission_SubmissionNotFound() {
        // Arrange
        when(submissionRepository.existsById("SUB_123456789")).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            submissionService.deleteSubmission("SUB_123456789");
        });

        verify(submissionRepository, never()).deleteById(anyString());
    }
} 