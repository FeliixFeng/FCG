package com.ghf.fcg.modules.medicine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.modules.medicine.entity.MedicineRecord;
import com.ghf.fcg.modules.medicine.mapper.MedicineRecordMapper;
import com.ghf.fcg.modules.medicine.service.IMedicineRecordService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class MedicineRecordServiceImpl extends ServiceImpl<MedicineRecordMapper, MedicineRecord> implements IMedicineRecordService {

    @Override
    public BigDecimal calculateComplianceRate(Long userId, LocalDate startDate, LocalDate endDate) {
        List<MedicineRecord> records = list(new LambdaQueryWrapper<MedicineRecord>()
                .eq(MedicineRecord::getUserId, userId)
                .ge(MedicineRecord::getScheduledDate, startDate)
                .le(MedicineRecord::getScheduledDate, endDate));

        if (records.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long takenCount = records.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == MedicineRecord.STATUS_TAKEN)
                .count();

        return BigDecimal.valueOf(takenCount * 100.0 / records.size())
                .setScale(1, RoundingMode.HALF_UP);
    }
}
