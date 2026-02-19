package com.ghf.fcg.modules.health.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.modules.health.entity.HealthReport;
import com.ghf.fcg.modules.health.mapper.HealthReportMapper;
import com.ghf.fcg.modules.health.service.IHealthReportService;
import org.springframework.stereotype.Service;

@Service
public class HealthReportServiceImpl extends ServiceImpl<HealthReportMapper, HealthReport> implements IHealthReportService {
}
