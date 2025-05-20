package com.agms.backend.controller;

import com.agms.backend.dto.CreateStudentRequest;
import com.agms.backend.entity.Student; // Assuming Student entity exists
import com.agms.backend.service.StudentService; // Assuming StudentService exists
import com.agms.backend.exception.ResourceNotFoundException;
import com.agms.backend.exception.EmailAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // Create a new student
    @PostMapping
    public ResponseEntity<?> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        try {
            Student createdStudent = studentService.createStudent(request);
            return new ResponseEntity<>(createdStudent, HttpStatus.CREATED);
        } catch (EmailAlreadyExistsException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // Get all students
    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        List<Student> students = studentService.getAllStudents();
        return new ResponseEntity<>(students, HttpStatus.OK);
    }

    // Get a student by ID
    @GetMapping("/{studentId}")
    public ResponseEntity<Student> getStudent(@PathVariable String studentId) {
        return studentService.getStudentByStudentId(studentId)
                .map(student -> new ResponseEntity<>(student, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Update a student
    @PutMapping("/{studentId}")
    public ResponseEntity<Student> updateStudent(
            @PathVariable String studentId,
            @Valid @RequestBody Student studentDetails) {
        try {
            Student updatedStudent = studentService.updateStudent(studentId, studentDetails);
            return new ResponseEntity<>(updatedStudent, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Delete a student
    @DeleteMapping("/{studentId}")
    public ResponseEntity<HttpStatus> deleteStudent(@PathVariable String studentId) {
        try {
            studentService.deleteStudent(studentId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{studentId}/graduation-status")
    public ResponseEntity<HttpStatus> updateGraduationStatus(
            @PathVariable String studentId,
            @RequestParam String status) {
        try {
            studentService.updateGraduationRequestStatus(studentId, status);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{studentId}/advisor/{advisorId}")
    public ResponseEntity<HttpStatus> assignAdvisor(
            @PathVariable String studentId,
            @PathVariable String advisorId) {
        try {
            studentService.assignAdvisor(studentId, advisorId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{studentId}/advisor")
    public ResponseEntity<HttpStatus> removeAdvisor(@PathVariable String studentId) {
        try {
            studentService.removeAdvisor(studentId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
