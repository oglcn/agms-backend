package com.agms.backend.controller;

import com.agms.backend.service.UbysService;
import com.agms.backend.model.users.Student;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ubys")
@RequiredArgsConstructor
@Tag(name = "UBYS", description = "APIs for UBYS student data operations")
public class UbysController {

    private final UbysService ubysService;

    @Operation(summary = "Get student data", description = "Retrieves student information from UBYS system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "400", description = "Invalid student number")
    })
    @GetMapping("/student/{studentNumber}")
    public ResponseEntity<?> getStudentData(
            @Parameter(description = "Student number", required = true) @PathVariable String studentNumber) {
        try {
            return ResponseEntity.ok(ubysService.getStudentData(studentNumber));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving student data: " + e.getMessage());
        }
    }

    @Operation(summary = "Get student with academic details", description = "Retrieves complete student information including GPA, courses, and academic performance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student data with academic details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "400", description = "Invalid student number")
    })
    @GetMapping("/student/{studentNumber}/complete")
    public ResponseEntity<?> getStudentWithTransientAttributes(
            @Parameter(description = "Student number", required = true) @PathVariable String studentNumber) {
        try {
            Student student = ubysService.getStudentWithTransientAttributes(studentNumber);
            return ResponseEntity.ok(student);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving complete student data: " + e.getMessage());
        }
    }

    @Operation(summary = "Get all students for database initialization", description = "Retrieves all students from UBYS system for database initialization (without transient attributes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All students retrieved successfully for database initialization"),
            @ApiResponse(responseCode = "500", description = "Error reading student data")
    })
    @GetMapping("/students/init")
    public ResponseEntity<?> getAllStudentsForDbInitialization() {
        try {
            List<Student> students = ubysService.getAllStudentsForDbInitialization();
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving students for database initialization: " + e.getMessage());
        }
    }

    @Operation(summary = "Check if student exists", description = "Validates if a student exists in the UBYS system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student existence check completed"),
            @ApiResponse(responseCode = "400", description = "Invalid student number")
    })
    @GetMapping("/student/{studentNumber}/exists")
    public ResponseEntity<?> checkStudentExists(
            @Parameter(description = "Student number", required = true) @PathVariable String studentNumber) {
        try {
            boolean exists = ubysService.studentExists(studentNumber);
            return ResponseEntity.ok(Map.of("exists", exists, "studentNumber", studentNumber));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking student existence: " + e.getMessage());
        }
    }
}
