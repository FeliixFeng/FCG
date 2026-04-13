package com.ghf.fcg.modules.medicine.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MedicineOcrEnhancedVO {

    private MedicineOcrParsedVO parsed;
    private String model;
    private Boolean fallback;
    private String aiRaw;
}
