package com.ghf.fcg.modules.medicine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.modules.medicine.entity.MedicinePlan;
import com.ghf.fcg.modules.medicine.mapper.MedicinePlanMapper;
import com.ghf.fcg.modules.medicine.service.IMedicinePlanService;
import org.springframework.stereotype.Service;

@Service
public class MedicinePlanServiceImpl extends ServiceImpl<MedicinePlanMapper, MedicinePlan> implements IMedicinePlanService {
}
