package com.ghf.fcg.modules.medicine.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MedicineOcrParsedVO {

    private String name;
    private String specification;
    private String manufacturer;
    private String dosageForm;
    private String instructions;
    private String contraindications;
    private String sideEffects;
    private String usage;
    private String dosage;
    private LocalDate expireDate;
}
