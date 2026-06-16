package com.cuidapp.catalogocuidapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OcrResponse {
    private String name;
    private String dosage;
    private String form;
    // 0.0 - 1.0: qué tan seguro está el parser de cada campo
    private Double nameConfidence;
    private Double dosageConfidence;
    private Double formConfidence;
    private String rawText;
}
