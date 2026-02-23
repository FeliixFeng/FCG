package com.ghf.fcg.modules.medicine.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MedicineVO {

    private Long id;
    private Long familyId;
    private String name;
    private String specification;
    private String manufacturer;
    private String dosageForm;
    private String imageUrl;
    private String instructions;
    private String contraindications;
    private String sideEffects;
    private Integer stock;
    private String stockUnit;
    private LocalDate expireDate;
    private String storageLocation;
    private LocalDateTime createTime;
}
