package com.ghf.fcg.modules.ai.service;

import com.ghf.fcg.modules.health.entity.HealthReport;
import com.ghf.fcg.modules.health.entity.Vital;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.entity.UserProfile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IAiContextService {

    String buildContextText(
            User user,
            LocalDate date,
            UserProfile profile,
            List<Map<String, Object>> todayTasks,
            List<Vital> todayVitals,
            Map<String, List<Vital>> weeklyVitals,
            HealthReport latestReport
    );

    List<Map<String, Object>> buildTodayTasks(Long userId, LocalDate date);

    List<Vital> listTodayVitals(Long familyId, Long userId, LocalDate date);

    Map<String, List<Vital>> buildWeeklyVitals(Long familyId, Long userId, LocalDate date);
}
