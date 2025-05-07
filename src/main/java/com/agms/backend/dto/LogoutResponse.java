// path: backend/src/main/java/com/agms/backend/dto/LogoutResponse.java
package com.agms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogoutResponse {
    private String message;
}