package com.agms.backend.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired; // Assuming Student entity exists
import org.springframework.http.HttpStatus; // Assuming StudentService exists
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agms.backend.dto.CreateStudentRequest;
import com.agms.backend.entity.Student;
import com.agms.backend.exception.EmailAlreadyExistsException;
import com.agms.backend.exception.ResourceNotFoundException;
import com.agms.backend.service.StudentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
@Tag(name = "Student Management", description = "APIs for managing students")
public class StudentController {

    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }


    @Operation(summary = "Create a new student", description = "Creates a new student with the provided information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Student created successfully",
            content = @Content(schema = @Schema(implementation = Student.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<?> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        try {
            Student createdStudent = studentService.createStudent(request);
            return new ResponseEntity<>(createdStudent, HttpStatus.CREATED);
        } catch (EmailAlreadyExistsException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }


    @Operation(summary = "Get all students", description = "Retrieves a list of all students")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all students",
        content = @Content(schema = @Schema(implementation = Student.class)))
    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        List<Student> students = studentService.getAllStudents();
        return new ResponseEntity<>(students, HttpStatus.OK);
    }


    @Operation(summary = "Get student by ID", description = "Retrieves a student by their student ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Student found successfully",
            content = @Content(schema = @Schema(implementation = Student.class))),
        @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/{studentId}")
    public ResponseEntity<Student> getStudent(
            @Parameter(description = "Student ID", required = true) @PathVariable String studentId) {
        return studentService.getStudentByStudentId(studentId)
                .map(student -> new ResponseEntity<>(student, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @Operation(summary = "Update student", description = "Updates an existing student's information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Student updated successfully",
            content = @Content(schema = @Schema(implementation = Student.class))),
        @ApiResponse(responseCode = "404", description = "Student not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping("/{studentId}")
    public ResponseEntity<Student> updateStudent(
            @Parameter(description = "Student ID", required = true) @PathVariable String studentId,
            @Valid @RequestBody Student studentDetails) {
        try {
            Student updatedStudent = studentService.updateStudent(studentId, studentDetails);
            return new ResponseEntity<>(updatedStudent, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @Operation(summary = "Delete student", description = "Deletes a student by their ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Student deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @DeleteMapping("/{studentId}")
    public ResponseEntity<HttpStatus> deleteStudent(
            @Parameter(description = "Student ID", required = true) @PathVariable String studentId) {
        try {
            studentService.deleteStudent(studentId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @Operation(summary = "Update graduation status", description = "Updates the graduation status of a student")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Graduation status updated successfully"),
        @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PutMapping("/{studentId}/graduation-status")
    public ResponseEntity<HttpStatus> updateGraduationStatus(
            @Parameter(description = "Student ID", required = true) @PathVariable String studentId,
            @Parameter(description = "New graduation status", required = true) @RequestParam String status) {
        try {
            studentService.updateGraduationRequestStatus(studentId, status);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @Operation(summary = "Assign advisor", description = "Assigns an advisor to a student")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Advisor assigned successfully"),
        @ApiResponse(responseCode = "404", description = "Student or advisor not found")
    })
    @PutMapping("/{studentId}/advisor/{advisorId}")
    public ResponseEntity<HttpStatus> assignAdvisor(
            @Parameter(description = "Student ID", required = true) @PathVariable String studentId,
            @Parameter(description = "Advisor ID", required = true) @PathVariable String advisorId) {
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
