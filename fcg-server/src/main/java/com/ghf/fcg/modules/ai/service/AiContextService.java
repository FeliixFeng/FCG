package com.ghf.fcg.modules.ai.service;

import com.ghf.fcg.modules.health.entity.HealthReport;
import com.ghf.fcg.modules.health.entity.Vital;
import com.ghf.fcg.modules.medicine.entity.MedicineRecord;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.entity.UserProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiContextService {

    public String buildContextText(
            User user,
            LocalDate date,
            UserProfile profile,
            List<Map<String, Object>> todayTasks,
            List<Vital> todayVitals,
            Map<String, List<Vital>> weeklyVitals,
            HealthReport latestReport
    ) {
        String memberLine = String.format("当前成员：%s（userId=%s）", safe(user != null ? user.getNickname() : null), user != null ? user.getId() : "-");
        String profileLine = profile == null
                ? "健康档案：暂无"
                : String.format("健康档案：身高%scm，体重%skg，病史%s，过敏%s",
                safe(profile.getHeight()), safe(profile.getWeight()), safe(profile.getDisease()), safe(profile.getAllergy()));

        long taken = todayTasks.stream().filter(t -> eq((Integer) t.get("status"), MedicineRecord.STATUS_TAKEN)).count();
        long skipped = todayTasks.stream().filter(t -> eq((Integer) t.get("status"), MedicineRecord.STATUS_SKIPPED)).count();
        long pending = todayTasks.size() - taken - skipped;
        String taskSummary = String.format("今日用药任务：共%d条，已打卡%d条，已跳过%d条，待处理%d条", todayTasks.size(), taken, skipped, pending);
        String taskDetail = todayTasks.isEmpty()
                ? "- 暂无任务"
                : todayTasks.stream().limit(10).map(this::formatTaskLine).collect(Collectors.joining("\n"));

        String todayVitalLine = todayVitals.isEmpty()
                ? "今日体征：暂无"
                : "今日体征：\n" + todayVitals.stream().map(this::formatVitalLine).collect(Collectors.joining("\n"));

        String weeklyLine = "近7天体征趋势：\n"
                + formatWeeklyTypeLine("血压", weeklyVitals.get("blood_pressure"))
                + "\n" + formatWeeklyTypeLine("空腹血糖", weeklyVitals.get("blood_sugar_fasting"))
                + "\n" + formatWeeklyTypeLine("餐后血糖", weeklyVitals.get("blood_sugar_postmeal"))
                + "\n" + formatWeeklyTypeLine("体重", weeklyVitals.get("weight"));

        String reportLine = latestReport == null
                ? "最新周报：暂无"
                : String.format("最新周报：风险等级%s，依从率%s%%", safe(latestReport.getRiskLevel()), safe(latestReport.getComplianceRate()));

        return String.join("\n", memberLine, "日期：" + date, profileLine, weeklyLine, taskSummary, taskDetail, todayVitalLine, reportLine);
    }

    private String formatTaskLine(Map<String, Object> task) {
        Integer status = (Integer) task.get("status");
        String statusText = switch (status == null ? -1 : status) {
            case 0 -> "待处理";
            case 1 -> "已打卡";
            case 2 -> "已跳过";
            default -> "未知";
        };
        return String.format("- %s %s%s（%s，%s）",
                safe(task.get("medicineName")),
                safe(task.get("dosage")),
                safe(task.get("unit")),
                safe(task.get("slot")),
                statusText
        );
    }

    private String formatVitalLine(Vital vital) {
        if (vital == null) {
            return "- 暂无";
        }
        if (eq(vital.getType(), Vital.TYPE_BLOOD_PRESSURE)) {
            return String.format("- 血压：%s/%s mmHg", safe(vital.getValueSystolic()), safe(vital.getValueDiastolic()));
        }
        if (eq(vital.getType(), Vital.TYPE_BLOOD_SUGAR)) {
            String label = eq(vital.getMeasurePoint(), Vital.POINT_AFTER_MEAL) ? "餐后血糖" : "空腹血糖";
            return String.format("- %s：%s mmol/L", label, safe(vital.getValue()));
        }
        if (eq(vital.getType(), Vital.TYPE_WEIGHT)) {
            return String.format("- 体重：%s kg", safe(vital.getValue()));
        }
        return String.format("- 类型%s：%s", safe(vital.getType()), safe(vital.getValue()));
    }

    private String formatWeeklyTypeLine(String label, List<Vital> list) {
        if (list == null || list.isEmpty()) {
            return "- " + label + "：近7天暂无记录";
        }
        Vital latest = list.get(0);
        if ("血压".equals(label)) {
            return String.format("- %s：近7天%d条，最新%s/%s mmHg", label, list.size(), safe(latest.getValueSystolic()), safe(latest.getValueDiastolic()));
        }
        String unit = "体重".equals(label) ? "kg" : "mmol/L";
        return String.format("- %s：近7天%d条，最新%s %s", label, list.size(), safe(latest.getValue()), unit);
    }

    private boolean eq(Integer a, int b) {
        return a != null && a == b;
    }

    private String safe(Object v) {
        if (v == null) {
            return "-";
        }
        if (v instanceof String s && s.isBlank()) {
            return "-";
        }
        if (v instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(v);
    }
}
