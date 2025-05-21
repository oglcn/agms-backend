package com.agms.backend.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.agms.backend.dto.ESignRequest;
import com.agms.backend.dto.ESignResponse;
import com.agms.backend.service.ESignService;

@Service
public class ESignServiceImpl implements ESignService {

    @Override
    public ESignResponse signESign(ESignRequest request) {
        // Generate current timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        
        // Generate eSignId by hashing the timestamp
        String eSignId = generateHash(timestamp);
        
        return ESignResponse.builder()
                .timestamp(timestamp)
                .eSignId(eSignId)
                .build();
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating hash", e);
        }
    }
} 