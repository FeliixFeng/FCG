package com.ghf.fcg.modules.medicine.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MedicineOcrParsedVO {

    private String name;
    private String specification;
    private LocalDate expireDate;
    private String usageNotes;
    private String indication;
    private String dosage;
}