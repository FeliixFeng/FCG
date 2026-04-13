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
    private String imageUrl;
    private Integer stock;
    private String stockUnit;
    private LocalDate expireDate;
    private String usageNotes;
    private String indication;
    private LocalDateTime createTime;
}