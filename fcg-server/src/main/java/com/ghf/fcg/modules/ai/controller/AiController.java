package com.ghf.fcg.modules.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.modules.ai.service.AiService;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.health.entity.HealthReport;
import com.ghf.fcg.modules.health.entity.Vital;
import com.ghf.fcg.modules.health.service.IHealthReportService;
import com.ghf.fcg.modules.health.service.IVitalService;
import com.ghf.fcg.modules.medicine.entity.Medicine;
import com.ghf.fcg.modules.medicine.entity.MedicinePlan;
import com.ghf.fcg.modules.medicine.entity.MedicineRecord;
import com.ghf.fcg.modules.medicine.service.IMedicinePlanService;
import com.ghf.fcg.modules.medicine.service.IMedicineRecordService;
import com.ghf.fcg.modules.medicine.service.IMedicineService;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.entity.UserProfile;
import com.ghf.fcg.modules.system.service.IUserProfileService;
import com.ghf.fcg.modules.system.service.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI智能助手", description = "AI大模型调用接口")
public class AiController {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final IUserService userService;
    private final IUserProfileService userProfileService;
    private final IVitalService vitalService;
    private final IHealthReportService healthReportService;
    private final IMedicinePlanService planService;
    private final IMedicineRecordService recordService;
    private final IMedicineService medicineService;
    private final Map<String, ContextCacheItem> contextCache = new ConcurrentHashMap<>();
    private static final long CONTEXT_CACHE_TTL_MS = 20_000L;

    @PostMapping("/chat")
    @Operation(summary = "AI对话")
    public Result<String> chat(@RequestBody ChatRequest request) {
        String response = aiService.chat(request.getSystemPrompt(), request.getUserPrompt());
        return Result.success(response);
    }

    @GetMapping("/context")
    @Operation(summary = "获取 AI 对话上下文", description = "聚合成员档案、今日任务、体征趋势、最新周报，并返回 contextText")
    public Result<Map<String, Object>> context(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);
        Long targetUserId = userId != null ? userId : currentUserId;
        validateFamilyUser(familyId, targetUserId);

        LocalDate targetDate = date != null ? date : LocalDate.now();
        String cacheKey = targetUserId + "_" + targetDate;
        ContextCacheItem cached = contextCache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.ts < CONTEXT_CACHE_TTL_MS) {
            return Result.success(cached.data);
        }

        User targetUser = userService.getById(targetUserId);
        UserProfile profile = userProfileService.getByUserId(targetUserId);
        List<Map<String, Object>> todayTasks = buildTodayTasks(targetUserId, targetDate);
        List<Vital> todayVitals = listTodayVitals(familyId, targetUserId, targetDate);
        Map<String, List<Vital>> weeklyVitals = buildWeeklyVitals(familyId, targetUserId, targetDate);
        HealthReport latestReport = healthReportService.getOne(new LambdaQueryWrapper<HealthReport>()
                .eq(HealthReport::getFamilyId, familyId)
                .eq(HealthReport::getUserId, targetUserId)
                .orderByDesc(HealthReport::getWeekStart)
                .last("LIMIT 1"));

        String contextText = buildContextText(targetUser, targetDate, profile, todayTasks, todayVitals, weeklyVitals, latestReport);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", targetUserId);
        data.put("nickname", targetUser != null ? targetUser.getNickname() : null);
        data.put("date", targetDate);
        data.put("profile", profile);
        data.put("todayTasks", todayTasks);
        data.put("todayVitals", todayVitals);
        data.put("weeklyVitals", weeklyVitals);
        data.put("latestReport", latestReport);
        data.put("contextText", contextText);
        log.info("AI context built: userId={}, date={}, tasks={}, todayVitals={}, weeklyBP={}, weeklyBSF={}, weeklyBSP={}, weeklyWeight={}, report={}",
                targetUserId,
                targetDate,
                todayTasks.size(),
                todayVitals.size(),
                sizeOf(weeklyVitals.get("blood_pressure")),
                sizeOf(weeklyVitals.get("blood_sugar_fasting")),
                sizeOf(weeklyVitals.get("blood_sugar_postmeal")),
                sizeOf(weeklyVitals.get("weight")),
                latestReport != null);
        contextCache.put(cacheKey, new ContextCacheItem(now, data));
        return Result.success(data);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI流式对话")
    public SseEmitter chatStream(@RequestBody ChatRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> log.debug("AI SSE completed"));
        emitter.onTimeout(() -> log.warn("AI SSE timeout"));

        CompletableFuture.runAsync(() -> {
            try {
                long startMs = System.currentTimeMillis();
                sendEvent(emitter, "start", null);
                int[] pieceCount = {0};
                long[] firstEmitMs = {-1L};
                aiService.streamChat(request.getSystemPrompt(), request.getUserPrompt(), delta -> {
                    if (firstEmitMs[0] < 0) {
                        firstEmitMs[0] = System.currentTimeMillis() - startMs;
                    }
                    pieceCount[0] += emitDeltaChunks(emitter, delta);
                });
                log.info("AI stream finished, firstEmit={}ms, emitted {} delta chunks", firstEmitMs[0], pieceCount[0]);
                sendEvent(emitter, "done", null);
                emitter.complete();
            } catch (Exception e) {
                log.error("AI stream failed: {}", e.getMessage(), e);
                sendEvent(emitter, "error", e.getMessage());
                emitter.complete();
            }
        });

        return emitter;
    }

    private int emitDeltaChunks(SseEmitter emitter, String delta) {
        if (delta == null || delta.isEmpty()) {
            return 0;
        }
        // 上游可能一次返回整段文本，这里按小块切片转发，保证前端流式观感稳定。
        final int chunkSize = 8;
        final long nanosPerChunk = 12_000_000L; // 12ms
        int count = 0;
        for (int i = 0; i < delta.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, delta.length());
            sendEvent(emitter, "delta", delta.substring(i, end));
            count += 1;
            // 避免网络层/代理层一次性合并太多数据，提升前端可见流式效果
            LockSupport.parkNanos(nanosPerChunk);
        }
        return count;
    }

    private void sendEvent(SseEmitter emitter, String type, String content) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            if (content != null) {
                payload.put("content", content);
            }
            String json = objectMapper.writeValueAsString(payload);
            emitter.send(SseEmitter.event().name("message").data(json));
        } catch (IOException e) {
            log.warn("AI SSE send IO failed, type={}, message={}", type, e.getMessage());
        } catch (Exception e) {
            log.warn("AI SSE send failed, type={}, message={}", type, e.getMessage());
        }
    }

    private Long requireFamilyId(Long userId) {
        User user = userService.getById(userId);
        if (user == null || user.getFamilyId() == null) {
            throw new BusinessException(MessageConstant.USER_NOT_IN_FAMILY);
        }
        return user.getFamilyId();
    }

    private void validateFamilyUser(Long familyId, Long userId) {
        User user = userService.getById(userId);
        if (user == null || !familyId.equals(user.getFamilyId())) {
            throw new BusinessException(MessageConstant.USER_FAMILY_MISMATCH);
        }
    }

    private List<Map<String, Object>> buildTodayTasks(Long userId, LocalDate date) {
        int day = date.getDayOfWeek().getValue();
        String dayToken = String.valueOf(day);

        List<MedicinePlan> plans = planService.list(new LambdaQueryWrapper<MedicinePlan>()
                .eq(MedicinePlan::getUserId, userId)
                .eq(MedicinePlan::getStatus, MedicinePlan.STATUS_ENABLED)
                .le(MedicinePlan::getStartDate, date)
                .and(w -> w.isNull(MedicinePlan::getEndDate).or().ge(MedicinePlan::getEndDate, date)));
        if (plans.isEmpty()) {
            return List.of();
        }

        List<MedicinePlan> validPlans = plans.stream()
                .filter(plan -> containsTakeDay(plan.getTakeDays(), dayToken))
                .toList();
        if (validPlans.isEmpty()) {
            return List.of();
        }

        Set<Long> medicineIds = validPlans.stream().map(MedicinePlan::getMedicineId).collect(Collectors.toSet());
        Map<Long, Medicine> medicineMap = medicineService.listByIds(medicineIds).stream()
                .collect(Collectors.toMap(Medicine::getId, m -> m));

        List<MedicineRecord> records = recordService.list(new LambdaQueryWrapper<MedicineRecord>()
                .eq(MedicineRecord::getUserId, userId)
                .eq(MedicineRecord::getScheduledDate, date));
        Map<String, MedicineRecord> recordMap = records.stream()
                .collect(Collectors.toMap(
                        r -> recordKey(r.getPlanId(), r.getSlotName()),
                        r -> r,
                        (a, b) -> a.getId() > b.getId() ? a : b
                ));

        List<Map<String, Object>> tasks = new java.util.ArrayList<>();
        for (MedicinePlan plan : validPlans) {
            Medicine medicine = medicineMap.get(plan.getMedicineId());
            for (String slot : parseSlots(plan.getRemindSlots())) {
                MedicineRecord record = recordMap.get(recordKey(plan.getId(), slot));
                Integer status = record != null ? record.getStatus() : MedicineRecord.STATUS_PENDING;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("planId", plan.getId());
                item.put("recordId", record != null ? record.getId() : null);
                item.put("medicineId", plan.getMedicineId());
                item.put("medicineName", medicine != null ? medicine.getName() : null);
                item.put("dosage", plan.getDosage());
                item.put("unit", medicine != null ? medicine.getStockUnit() : null);
                item.put("slot", slot);
                item.put("status", status);
                tasks.add(item);
            }
        }
        return tasks;
    }

    private List<Vital> listTodayVitals(Long familyId, Long userId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        List<Vital> today = vitalService.list(new LambdaQueryWrapper<Vital>()
                .eq(Vital::getFamilyId, familyId)
                .eq(Vital::getUserId, userId)
                .ge(Vital::getMeasureTime, dayStart)
                .le(Vital::getMeasureTime, dayEnd)
                .orderByDesc(Vital::getMeasureTime));
        if (today.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<Vital> result = new java.util.ArrayList<>();
        for (Vital v : today) {
            String key = v.getType() != null && v.getType().equals(Vital.TYPE_BLOOD_SUGAR)
                    ? v.getType() + "_" + v.getMeasurePoint() + "_" + v.getId()
                    : String.valueOf(v.getType());
            if (v.getType() != null && v.getType().equals(Vital.TYPE_BLOOD_SUGAR)) {
                result.add(v);
                continue;
            }
            if (seen.add(key)) {
                result.add(v);
            }
        }
        return result;
    }

    private Map<String, List<Vital>> buildWeeklyVitals(Long familyId, Long userId, LocalDate date) {
        LocalDateTime start = date.minusDays(6).atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<Vital> all = vitalService.list(new LambdaQueryWrapper<Vital>()
                .eq(Vital::getFamilyId, familyId)
                .eq(Vital::getUserId, userId)
                .ge(Vital::getMeasureTime, start)
                .le(Vital::getMeasureTime, end)
                .orderByDesc(Vital::getMeasureTime));

        Map<String, List<Vital>> map = new LinkedHashMap<>();
        map.put("blood_pressure", all.stream().filter(v -> eq(v.getType(), Vital.TYPE_BLOOD_PRESSURE)).toList());
        map.put("blood_sugar_fasting", all.stream().filter(v -> eq(v.getType(), Vital.TYPE_BLOOD_SUGAR) && eq(v.getMeasurePoint(), Vital.POINT_FASTING)).toList());
        map.put("blood_sugar_postmeal", all.stream().filter(v -> eq(v.getType(), Vital.TYPE_BLOOD_SUGAR) && eq(v.getMeasurePoint(), Vital.POINT_AFTER_MEAL)).toList());
        map.put("weight", all.stream().filter(v -> eq(v.getType(), Vital.TYPE_WEIGHT)).toList());
        return map;
    }

    private String buildContextText(
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

    private List<String> parseSlots(String remindSlots) {
        if (remindSlots == null || remindSlots.isBlank()) {
            return List.of("08:00");
        }
        return List.of(remindSlots.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private boolean containsTakeDay(String takeDays, String dayToken) {
        if (takeDays == null || takeDays.isBlank()) {
            return true;
        }
        List<String> tokens = List.of(takeDays.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return tokens.contains(dayToken);
    }

    private String recordKey(Long planId, String slotName) {
        return planId + "_" + (slotName == null ? "" : slotName.trim());
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

    private int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static class ContextCacheItem {
        private final long ts;
        private final Map<String, Object> data;

        private ContextCacheItem(long ts, Map<String, Object> data) {
            this.ts = ts;
            this.data = data;
        }
    }

    // ========== DTO ==========

    public static class ChatRequest {
        private String systemPrompt;
        private String userPrompt;

        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public String getUserPrompt() { return userPrompt; }
        public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
    }
}
