package com.agms.backend.service.impl;

import com.agms.backend.dto.CreateSubmissionRequest;
import com.agms.backend.dto.SubmissionResponse;
import com.agms.backend.exception.ResourceNotFoundException;
import com.agms.backend.model.AdvisorList;
import com.agms.backend.model.File;
import com.agms.backend.model.Submission;
import com.agms.backend.model.SubmissionStatus;
import com.agms.backend.model.users.Advisor;
import com.agms.backend.model.users.Student;
import com.agms.backend.repository.AdvisorListRepository;
import com.agms.backend.repository.AdvisorRepository;
import com.agms.backend.repository.StudentRepository;
import com.agms.backend.repository.SubmissionRepository;
import com.agms.backend.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final StudentRepository studentRepository;
    private final AdvisorRepository advisorRepository;
    private final AdvisorListRepository advisorListRepository;

    @Override
    @Transactional
    public SubmissionResponse createGraduationSubmission(CreateSubmissionRequest request) {
        log.info("Creating graduation submission for student: {}", request.getStudentNumber());
        
        // Find the student
        Student student = studentRepository.findByStudentNumber(request.getStudentNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with number: " + request.getStudentNumber()));
        
        // Check if student already has a pending submission
        if (hasActivePendingSubmission(request.getStudentNumber())) {
            throw new IllegalStateException("Student already has an active pending graduation submission");
        }
        
        // Find the student's advisor
        Advisor advisor = student.getAdvisor();
        if (advisor == null) {
            throw new IllegalStateException("Student does not have an assigned advisor");
        }
        
        // Find the advisor's advisor list
        AdvisorList advisorList = advisor.getAdvisorList();
        if (advisorList == null) {
            throw new IllegalStateException("Advisor does not have an advisor list configured");
        }
        
        // Create the submission (ID will be generated)
        String submissionId = generateSubmissionId();
        Submission submission = Submission.builder()
                .submissionId(submissionId)
                .submissionDate(LocalDate.now())
                .content(request.getContent())
                .status(SubmissionStatus.PENDING)
                .student(student)
                .advisorList(advisorList)
                .build();
        
        // Save the submission
        Submission savedSubmission = submissionRepository.save(submission);
        
        log.info("Created graduation submission with ID: {} for student: {}", savedSubmission.getSubmissionId(), request.getStudentNumber());
        
        return convertToResponse(savedSubmission);
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByStudent(String studentNumber) {
        log.debug("Getting submissions for student: {}", studentNumber);
        
        List<Submission> submissions = submissionRepository.findByStudentNumber(studentNumber);
        return submissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByAdvisor(String advisorEmpId) {
        log.debug("Getting submissions for advisor: {}", advisorEmpId);
        
        // Find the advisor
        Advisor advisor = advisorRepository.findByEmpId(advisorEmpId)
                .orElseThrow(() -> new ResourceNotFoundException("Advisor not found with empId: " + advisorEmpId));
        
        // Find the advisor's advisor list
        AdvisorList advisorList = advisor.getAdvisorList();
        if (advisorList == null) {
            log.warn("Advisor {} does not have an advisor list", advisorEmpId);
            return List.of();
        }
        
        List<Submission> submissions = submissionRepository.findByAdvisorListId(advisorList.getAdvisorListId());
        return submissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SubmissionResponse> getSubmissionById(String submissionId) {
        log.debug("Getting submission by ID: {}", submissionId);
        
        return submissionRepository.findById(submissionId)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional
    public SubmissionResponse updateSubmissionStatus(String submissionId, SubmissionStatus status) {
        log.info("Updating submission {} status to: {}", submissionId, status);
        
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + submissionId));
        
        submission.setStatus(status);
        Submission updatedSubmission = submissionRepository.save(submission);
        
        log.info("Updated submission {} status to: {}", submissionId, status);
        
        return convertToResponse(updatedSubmission);
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByStatus(SubmissionStatus status) {
        log.debug("Getting submissions with status: {}", status);
        
        List<Submission> submissions = submissionRepository.findByStatus(status);
        return submissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasActivePendingSubmission(String studentNumber) {
        return submissionRepository.hasActivePendingSubmission(studentNumber);
    }

    @Override
    public Optional<SubmissionResponse> getLatestSubmissionByStudent(String studentNumber) {
        log.debug("Getting latest submission for student: {}", studentNumber);
        
        List<Submission> submissions = submissionRepository.findByStudentNumberOrderBySubmissionDateDesc(studentNumber);
        if (submissions.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(convertToResponse(submissions.get(0)));
    }

    @Override
    @Transactional
    public void deleteSubmission(String submissionId) {
        log.info("Deleting submission: {}", submissionId);
        
        if (!submissionRepository.existsById(submissionId)) {
            throw new ResourceNotFoundException("Submission not found with ID: " + submissionId);
        }
        
        submissionRepository.deleteById(submissionId);
        
        log.info("Deleted submission: {}", submissionId);
    }

    private SubmissionResponse convertToResponse(Submission submission) {
        Student student = submission.getStudent();
        String studentName = student.getFirstName() + " " + student.getLastName();
        
        List<SubmissionResponse.FileInfo> fileInfos = List.of();
        if (submission.getFiles() != null) {
            fileInfos = submission.getFiles().stream()
                    .map(file -> SubmissionResponse.FileInfo.builder()
                            .fileId(file.getFileId().toString())
                            .fileName(file.getFileName())
                            .fileType(file.getFileType())
                            .uploadDate(file.getUploadDate())
                            .uploaderName(file.getUploader().getFirstName() + " " + file.getUploader().getLastName())
                            .build())
                    .collect(Collectors.toList());
        }
        
        return SubmissionResponse.builder()
                .submissionId(submission.getSubmissionId())
                .submissionDate(submission.getSubmissionDate())
                .content(submission.getContent())
                .status(submission.getStatus())
                .studentNumber(student.getStudentNumber())
                .studentName(studentName)
                .advisorListId(submission.getAdvisorList() != null ? submission.getAdvisorList().getAdvisorListId() : null)
                .files(fileInfos)
                .build();
    }

    private String generateSubmissionId() {
        // Generate a submission ID like "SUB_" + timestamp
        return "SUB_" + System.currentTimeMillis();
    }
} 