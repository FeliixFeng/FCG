package com.ghf.fcg.modules.health.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.modules.health.entity.Vital;
import com.ghf.fcg.modules.health.mapper.VitalMapper;
import com.ghf.fcg.modules.health.service.IVitalService;
import org.springframework.stereotype.Service;

@Service
public class VitalServiceImpl extends ServiceImpl<VitalMapper, Vital> implements IVitalService {
}
