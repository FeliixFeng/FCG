package com.ghf.fcg.modules.health.service.impl;

import com.ghf.fcg.modules.health.entity.HealthReport;
import com.ghf.fcg.modules.health.entity.Vital;
import com.ghf.fcg.modules.health.service.IHealthReportAnalysisService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class HealthReportAnalysisServiceImpl implements IHealthReportAnalysisService {

    @Override
    public String formatVitalData(List<Vital> vitals) {
        if (vitals == null || vitals.isEmpty()) {
            return "本周暂无体征数据";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【本周体征结构化摘要】\n");
        sb.append("总记录数：").append(vitals.size()).append(" 条\n");

        List<Vital> bpList = vitals.stream()
                .filter(v -> v.getType() == Vital.TYPE_BLOOD_PRESSURE && v.getValueSystolic() != null && v.getValueDiastolic() != null)
                .sorted(Comparator.comparing(Vital::getMeasureTime))
                .toList();
        if (!bpList.isEmpty()) {
            Vital latest = bpList.get(bpList.size() - 1);
            double avgSys = bpList.stream().map(Vital::getValueSystolic).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            double avgDia = bpList.stream().map(Vital::getValueDiastolic).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            long abnormal = bpList.stream().filter(this::isBpAbnormal).count();
            sb.append("- 血压：").append(bpList.size()).append(" 次；")
                    .append("最新 ").append(latest.getValueSystolic()).append("/").append(latest.getValueDiastolic()).append(" mmHg；")
                    .append("均值 ").append(round1(avgSys)).append("/").append(round1(avgDia)).append(" mmHg；")
                    .append("异常 ").append(abnormal).append(" 次\n");
        }

        List<Vital> fastingBs = vitals.stream()
                .filter(v -> v.getType() == Vital.TYPE_BLOOD_SUGAR && v.getValue() != null && Integer.valueOf(Vital.POINT_FASTING).equals(v.getMeasurePoint()))
                .sorted(Comparator.comparing(Vital::getMeasureTime))
                .toList();
        if (!fastingBs.isEmpty()) {
            Vital latest = fastingBs.get(fastingBs.size() - 1);
            double avg = fastingBs.stream().map(Vital::getValue).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            long abnormal = fastingBs.stream().filter(this::isBloodSugarAbnormal).count();
            sb.append("- 血糖（空腹）：").append(fastingBs.size()).append(" 次；")
                    .append("最新 ").append(latest.getValue()).append(" mmol/L；")
                    .append("均值 ").append(round1(avg)).append(" mmol/L；")
                    .append("异常 ").append(abnormal).append(" 次\n");
        }

        List<Vital> postMealBs = vitals.stream()
                .filter(v -> v.getType() == Vital.TYPE_BLOOD_SUGAR && v.getValue() != null && Integer.valueOf(Vital.POINT_AFTER_MEAL).equals(v.getMeasurePoint()))
                .sorted(Comparator.comparing(Vital::getMeasureTime))
                .toList();
        if (!postMealBs.isEmpty()) {
            Vital latest = postMealBs.get(postMealBs.size() - 1);
            double avg = postMealBs.stream().map(Vital::getValue).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            long abnormal = postMealBs.stream().filter(this::isBloodSugarAbnormal).count();
            sb.append("- 血糖（餐后）：").append(postMealBs.size()).append(" 次；")
                    .append("最新 ").append(latest.getValue()).append(" mmol/L；")
                    .append("均值 ").append(round1(avg)).append(" mmol/L；")
                    .append("异常 ").append(abnormal).append(" 次\n");
        }

        List<Vital> weightList = vitals.stream()
                .filter(v -> v.getType() == Vital.TYPE_WEIGHT && v.getValue() != null)
                .sorted(Comparator.comparing(Vital::getMeasureTime))
                .toList();
        if (!weightList.isEmpty()) {
            Vital latest = weightList.get(weightList.size() - 1);
            Vital first = weightList.get(0);
            double avg = weightList.stream().map(Vital::getValue).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            double delta = latest.getValue().doubleValue() - first.getValue().doubleValue();
            sb.append("- 体重：").append(weightList.size()).append(" 次；")
                    .append("最新 ").append(latest.getValue()).append(" kg；")
                    .append("均值 ").append(round1(avg)).append(" kg；")
                    .append("本周变化 ").append(delta > 0 ? "+" : "").append(round1(delta)).append(" kg\n");
        }

        return sb.toString();
    }

    @Override
    public int analyzeRiskLevel(List<Vital> vitals, BigDecimal complianceRate) {
        if (complianceRate == null) {
            return HealthReport.RISK_LOW;
        }

        double rate = complianceRate.doubleValue();
        long abnormalCount = vitals.stream().filter(this::isVitalAbnormal).count();
        boolean severeAbnormal = vitals.stream().anyMatch(this::isVitalSeverelyAbnormal);

        if (rate < 60 || severeAbnormal) {
            return HealthReport.RISK_HIGH;
        } else if (rate < 80 || abnormalCount >= 3) {
            return HealthReport.RISK_MEDIUM;
        }

        return HealthReport.RISK_LOW;
    }

    @Override
    public String resolveAiErrorMessage(RuntimeException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        String lower = msg.toLowerCase();

        if (lower.contains("401") || lower.contains("unauthorized") || lower.contains("invalid api key")) {
            return "AI 服务认证失败（API Key 无效或已过期），请检查后端配置";
        }
        if (lower.contains("403")) {
            return "AI 服务无调用权限，请检查账号或模型权限配置";
        }
        if (lower.contains("429") || lower.contains("quota") || lower.contains("rate limit")) {
            return "AI 服务调用受限（额度不足或请求过快），请稍后重试";
        }
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return "AI 服务请求超时，请检查网络后重试";
        }
        if (lower.contains("connection refused") || lower.contains("connect") || lower.contains("network")) {
            return "AI 服务连接失败，请检查网络或服务地址配置";
        }
        return "AI 周报生成失败，请稍后重试（可检查模型配置和网络）";
    }

    private boolean isVitalAbnormal(Vital vital) {
        if (vital == null) return false;
        if (vital.getType() == Vital.TYPE_BLOOD_PRESSURE) {
            return isBpAbnormal(vital);
        }
        if (vital.getType() == Vital.TYPE_BLOOD_SUGAR) {
            return isBloodSugarAbnormal(vital);
        }
        return false;
    }

    private boolean isVitalSeverelyAbnormal(Vital vital) {
        if (vital == null) return false;
        if (vital.getType() == Vital.TYPE_BLOOD_PRESSURE && vital.getValueSystolic() != null && vital.getValueDiastolic() != null) {
            return vital.getValueSystolic().compareTo(BigDecimal.valueOf(180)) >= 0
                    || vital.getValueDiastolic().compareTo(BigDecimal.valueOf(120)) >= 0;
        }
        if (vital.getType() == Vital.TYPE_BLOOD_SUGAR && vital.getValue() != null) {
            return vital.getValue().compareTo(BigDecimal.valueOf(2.2)) < 0
                    || vital.getValue().compareTo(BigDecimal.valueOf(16)) >= 0;
        }
        return false;
    }

    private boolean isBpAbnormal(Vital v) {
        if (v.getValueSystolic() == null || v.getValueDiastolic() == null) return false;
        return v.getValueSystolic().compareTo(BigDecimal.valueOf(140)) >= 0
                || v.getValueSystolic().compareTo(BigDecimal.valueOf(90)) < 0
                || v.getValueDiastolic().compareTo(BigDecimal.valueOf(90)) >= 0
                || v.getValueDiastolic().compareTo(BigDecimal.valueOf(60)) < 0;
    }

    private boolean isBloodSugarAbnormal(Vital v) {
        if (v.getValue() == null) return false;
        BigDecimal value = v.getValue();
        if (Integer.valueOf(Vital.POINT_FASTING).equals(v.getMeasurePoint())) {
            return value.compareTo(BigDecimal.valueOf(3.9)) < 0 || value.compareTo(BigDecimal.valueOf(6.1)) > 0;
        }
        if (Integer.valueOf(Vital.POINT_AFTER_MEAL).equals(v.getMeasurePoint())) {
            return value.compareTo(BigDecimal.valueOf(3.9)) < 0 || value.compareTo(BigDecimal.valueOf(7.8)) > 0;
        }
        return value.compareTo(BigDecimal.valueOf(3.9)) < 0 || value.compareTo(BigDecimal.valueOf(7.8)) > 0;
    }

    private String round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }
}
