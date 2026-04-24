package com.ghf.fcg.modules.health.service;

import com.ghf.fcg.modules.health.entity.Vital;

import java.math.BigDecimal;
import java.util.List;

public interface IHealthReportAnalysisService {

    String formatVitalData(List<Vital> vitals);

    int analyzeRiskLevel(List<Vital> vitals, BigDecimal complianceRate);

    String resolveAiErrorMessage(RuntimeException e);
}
