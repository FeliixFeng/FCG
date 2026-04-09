package com.ghf.fcg.modules.medicine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghf.fcg.modules.medicine.entity.MedicineRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface IMedicineRecordService extends IService<MedicineRecord> {

    /**
     * 计算指定用户指定日期范围内的用药依从率
     * @return 依从率 (0-100)
     */
    BigDecimal calculateComplianceRate(Long userId, LocalDate startDate, LocalDate endDate);
}
