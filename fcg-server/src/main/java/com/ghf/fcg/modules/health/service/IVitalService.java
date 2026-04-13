package com.ghf.fcg.modules.health.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghf.fcg.modules.health.entity.Vital;

import java.time.LocalDateTime;
import java.util.List;

public interface IVitalService extends IService<Vital> {

    /**
     * 查询指定时间范围内的体征数据
     */
    List<Vital> listByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);
}
