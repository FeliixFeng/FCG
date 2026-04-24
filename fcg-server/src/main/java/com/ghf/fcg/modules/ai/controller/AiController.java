package com.ghf.fcg.modules.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.modules.ai.service.AiService;
import com.ghf.fcg.modules.ai.service.IAiContextService;
import com.ghf.fcg.modules.ai.dto.ChatDTO;
import com.ghf.fcg.modules.ai.vo.AiContextVO;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.health.entity.HealthReport;
import com.ghf.fcg.modules.health.entity.Vital;
import com.ghf.fcg.modules.health.service.IHealthReportService;
import com.ghf.fcg.modules.medicine.entity.MedicineRecord;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

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
    private final IHealthReportService healthReportService;
    private final IAiContextService aiContextService;
    private final Map<String, ContextCacheItem> contextCache = new ConcurrentHashMap<>();
    private static final long CONTEXT_CACHE_TTL_MS = 20_000L;

    @PostMapping("/chat")
    @Operation(summary = "AI对话")
    public Result<String> chat(@RequestBody ChatDTO request) {
        String response = aiService.chat(request.getSystemPrompt(), request.getUserPrompt());
        return Result.success(response);
    }

    @GetMapping("/context")
    @Operation(summary = "获取 AI 对话上下文", description = "聚合成员档案、今日任务、体征趋势、最新周报，并返回 contextText")
    public Result<AiContextVO> context(
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
        List<Map<String, Object>> todayTasks = aiContextService.buildTodayTasks(targetUserId, targetDate);
        List<Vital> todayVitals = aiContextService.listTodayVitals(familyId, targetUserId, targetDate);
        Map<String, List<Vital>> weeklyVitals = aiContextService.buildWeeklyVitals(familyId, targetUserId, targetDate);
        HealthReport latestReport = healthReportService.getOne(new LambdaQueryWrapper<HealthReport>()
                .eq(HealthReport::getFamilyId, familyId)
                .eq(HealthReport::getUserId, targetUserId)
                .orderByDesc(HealthReport::getWeekStart)
                .last("LIMIT 1"));

        String contextText = aiContextService.buildContextText(targetUser, targetDate, profile, todayTasks, todayVitals, weeklyVitals, latestReport);

        AiContextVO data = new AiContextVO();
        data.setUserId(targetUserId);
        data.setNickname(targetUser != null ? targetUser.getNickname() : null);
        data.setDate(targetDate);
        data.setProfile(profile);
        data.setTodayTasks(todayTasks);
        data.setTodayVitals(todayVitals);
        data.setWeeklyVitals(weeklyVitals);
        data.setLatestReport(latestReport);
        data.setContextText(contextText);
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
    public SseEmitter chatStream(@RequestBody ChatDTO request, HttpServletResponse response) {
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

    private boolean eq(Integer a, int b) {
        return a != null && a == b;
    }

    private int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static class ContextCacheItem {
        private final long ts;
        private final AiContextVO data;

        private ContextCacheItem(long ts, AiContextVO data) {
            this.ts = ts;
            this.data = data;
        }
    }
}
