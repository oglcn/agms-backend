package com.agms.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agms.backend.dto.ESignRequest;
import com.agms.backend.dto.ESignResponse;
import com.agms.backend.service.ESignService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/e-sign")
public class ESignController {

    private final ESignService eSignService;

    @Autowired
    public ESignController(ESignService eSignService) {
        this.eSignService = eSignService;
    }

    @PostMapping("/sign")
    @Operation(summary = "Sign a document electronically")
    public ResponseEntity<ESignResponse> signESign(@Valid @RequestBody ESignRequest request) {
        ESignResponse response = eSignService.signESign(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}