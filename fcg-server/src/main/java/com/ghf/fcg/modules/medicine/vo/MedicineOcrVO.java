package com.ghf.fcg.modules.medicine.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MedicineOcrVO {
    private String text;
    private List<String> lines;
    private String error;
}
