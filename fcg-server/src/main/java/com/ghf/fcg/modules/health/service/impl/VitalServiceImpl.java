package com.ghf.fcg.modules.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.modules.health.entity.Vital;
import com.ghf.fcg.modules.health.mapper.VitalMapper;
import com.ghf.fcg.modules.health.service.IVitalService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VitalServiceImpl extends ServiceImpl<VitalMapper, Vital> implements IVitalService {

    @Override
    public List<Vital> listByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return list(new LambdaQueryWrapper<Vital>()
                .eq(Vital::getUserId, userId)
                .ge(Vital::getMeasureTime, startTime)
                .le(Vital::getMeasureTime, endTime)
                .orderByAsc(Vital::getMeasureTime));
    }
}
