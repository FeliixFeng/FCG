package com.ghf.fcg.modules.medicine.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MedicineOcrEnhancedVO {

    private String rawText;
    private List<String> rawLines;
    private String ocrError;
    private MedicineOcrParsedVO parsed;
    private String model;
    private Boolean fallback;
    private String aiRaw;
}
