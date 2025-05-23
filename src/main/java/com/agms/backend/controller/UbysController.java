package com.agms.backend.controller;

import com.agms.backend.service.UbysService;
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
}
