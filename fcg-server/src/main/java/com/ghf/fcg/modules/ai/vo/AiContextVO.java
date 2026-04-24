package com.ghf.fcg.modules.ai.vo;

import com.ghf.fcg.modules.health.entity.HealthReport;
import com.ghf.fcg.modules.health.entity.Vital;
import com.ghf.fcg.modules.system.entity.UserProfile;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class AiContextVO {

    private Long userId;
    private String nickname;
    private LocalDate date;
    private UserProfile profile;
    private List<Map<String, Object>> todayTasks;
    private List<Vital> todayVitals;
    private Map<String, List<Vital>> weeklyVitals;
    private HealthReport latestReport;
    private String contextText;
}
