package com.smartreport.models.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank
    private String companyCode;

    @NotBlank
    private String message;

    private String sessionId;
}
