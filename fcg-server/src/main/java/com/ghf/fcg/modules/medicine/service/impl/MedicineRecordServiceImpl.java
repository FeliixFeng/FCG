package com.ghf.fcg.modules.medicine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.modules.medicine.entity.MedicineRecord;
import com.ghf.fcg.modules.medicine.mapper.MedicineRecordMapper;
import com.ghf.fcg.modules.medicine.service.IMedicineRecordService;
import org.springframework.stereotype.Service;

@Service
public class MedicineRecordServiceImpl extends ServiceImpl<MedicineRecordMapper, MedicineRecord> implements IMedicineRecordService {
}
