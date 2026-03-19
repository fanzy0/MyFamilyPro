package com.my.family.paxl.scheduled;

import com.my.family.paxl.domain.entity.EventDO;
import com.my.family.paxl.domain.entity.FamilyMemberDO;
import com.my.family.paxl.domain.entity.RemindLogDO;
import com.my.family.paxl.mapper.EventMapper;
import com.my.family.paxl.mapper.FamilyMemberMapper;
import com.my.family.paxl.mapper.RemindLogMapper;
import com.my.family.paxl.util.LunarCalendarUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提醒记录定时扫描任务
 * 每天 07:00 执行：生成当天应提醒的日志记录，并关闭已过期的未处理记录
 *
 * @author ai
 * @date 2026/03/18
 */
@Component
@Slf4j
public class RemindScanJob {

    /**
     * 批量关闭时每批最大条数，避免单次 SQL 过长
     */
    private static final int BATCH_CLOSE_SIZE = 100;

    private final EventMapper eventMapper;
    private final FamilyMemberMapper familyMemberMapper;
    private final RemindLogMapper remindLogMapper;
    private final LunarCalendarUtil lunarCalendarUtil;

    public RemindScanJob(EventMapper eventMapper,
                         FamilyMemberMapper familyMemberMapper,
                         RemindLogMapper remindLogMapper,
                         LunarCalendarUtil lunarCalendarUtil) {
        this.eventMapper = eventMapper;
        this.familyMemberMapper = familyMemberMapper;
        this.remindLogMapper = remindLogMapper;
        this.lunarCalendarUtil = lunarCalendarUtil;
    }

    /**
     * 主入口：每天 07:00 触发（cron: 秒 分 时 日 月 星期）
     */
    @Scheduled(cron = "0 37 14 * * ?")
    public void scanAndGenerate() {
        log.info("[RemindScanJob] 定时任务开始执行, time={}", LocalDateTime.now());
        try {
            generateRemindLogs();
            closeExpiredLogs();
        } catch (Exception e) {
            log.error("[RemindScanJob] 任务执行异常", e);
        }
        log.info("[RemindScanJob] 定时任务执行完毕, time={}", LocalDateTime.now());
    }

    /**
     * 生成当天应提醒的日志记录
     *
     * <p>步骤：
     * <ol>
     *   <li>批量查询全量有效事项（remind_enabled=1 AND status=0 AND deleted=0）</li>
     *   <li>按 familyId 分组，一次性批量查询相关家庭的所有 APPROVED 成员</li>
     *   <li>逐个事项计算当年提醒窗口，判断今天是否落在窗口内</li>
     *   <li>命中则构造 RemindLogDO，收集后批量 INSERT IGNORE 去重插入</li>
     * </ol>
     * </p>
     */
    private void generateRemindLogs() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        log.info("[GenerateRemindLogs] 开始生成提醒日志, today={}", today);

        // 1. 查询全量有效事项（两次数据库操作：一次查事项，一次查成员）
        List<EventDO> events = eventMapper.selectAllActiveWithRemind();
        if (events == null || events.isEmpty()) {
            log.info("[GenerateRemindLogs] 暂无开启提醒的事项，跳过");
            return;
        }
        log.info("[GenerateRemindLogs] 共发现 {} 个开启提醒的事项", events.size());

        // 2. 收集所有涉及的 familyId，批量查询各家庭的 APPROVED 成员
        List<Long> familyIds = events.stream()
                .map(EventDO::getFamilyId)
                .distinct()
                .collect(Collectors.toList());

        List<FamilyMemberDO> allMembers = familyMemberMapper.selectApprovedMembersByFamilyIds(familyIds);

        // 3. 按 familyId → List<userId> 建立内存索引，避免在循环中查数据库
        Map<Long, List<Long>> familyUserMap = new HashMap<>();
        for (FamilyMemberDO member : allMembers) {
            familyUserMap
                    .computeIfAbsent(member.getFamilyId(), k -> new ArrayList<>())
                    .add(member.getUserId());
        }

        // 4. 按家庭分组处理：每个家庭的事项独立收集、独立插入
        //    好处：某个家庭的插入异常不影响其他家庭的处理
        Map<Long, List<EventDO>> eventsByFamily = new HashMap<>();
        for (EventDO event : events) {
            eventsByFamily
                    .computeIfAbsent(event.getFamilyId(), k -> new ArrayList<>())
                    .add(event);
        }

        int totalAttempted = 0;
        int totalInserted = 0;
        for (Map.Entry<Long, List<EventDO>> entry : eventsByFamily.entrySet()) {
            Long familyId = entry.getKey();
            List<EventDO> familyEvents = entry.getValue();
            List<RemindLogDO> logsToInsert = new ArrayList<>();

            for (EventDO event : familyEvents) {
                try {
                    processEventForGenerate(event, today, currentYear, familyUserMap, logsToInsert);
                } catch (Exception e) {
                    log.error("[GenerateRemindLogs] 处理事项异常，跳过. eventId={}, title={}",
                            event.getId(), event.getTitle(), e);
                }
            }

            if (logsToInsert.isEmpty()) {
                continue;
            }

            // 5. 按家庭独立执行批量 INSERT IGNORE，异常时仅影响当前家庭
            try {
                int inserted = remindLogMapper.batchInsertIgnore(logsToInsert);
                totalAttempted += logsToInsert.size();
                totalInserted += inserted;
                log.info("[GenerateRemindLogs] 家庭插入完成, familyId={}, 待插入={}, 实际新增={}",
                        familyId, logsToInsert.size(), inserted);
            } catch (Exception e) {
                log.error("[GenerateRemindLogs] 家庭批量插入异常，跳过该家庭，下次任务重试. familyId={}", familyId, e);
            }
        }

        log.info("[GenerateRemindLogs] 全部完成, 总尝试={}, 总新增={}", totalAttempted, totalInserted);
    }

    /**
     * 处理单个事项，判断是否应在今日生成提醒记录
     */
    private void processEventForGenerate(EventDO event,
                                         LocalDate today,
                                         int currentYear,
                                         Map<Long, List<Long>> familyUserMap,
                                         List<RemindLogDO> logsToInsert) {
        // 计算当年事件实际日期（阳历）
        LocalDate eventDateThisYear = calcEventDate(event, currentYear);
        if (eventDateThisYear == null) {
            log.warn("[ProcessEvent] 无法计算事件日期，跳过. eventId={}", event.getId());
            return;
        }

        // 计算应开始提醒日期
        int advanceDays = event.getRemindAdvanceDays() != null ? event.getRemindAdvanceDays() : 0;
        LocalDate remindDateThisYear = eventDateThisYear.minusDays(advanceDays);

        // 判断今天是否在提醒窗口内：remindDate <= today <= eventDate
        if (today.isBefore(remindDateThisYear) || today.isAfter(eventDateThisYear)) {
            log.debug("[ProcessEvent] 不在提醒窗口内，跳过. eventId={}, remindDate={}, eventDate={}, today={}",
                    event.getId(), remindDateThisYear, eventDateThisYear, today);
            return;
        }

        // 确定提醒对象
        List<Long> targetUserIds = resolveTargetUserIds(event, familyUserMap);
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            log.warn("[ProcessEvent] 未找到提醒对象，跳过. eventId={}, remindTarget={}",
                    event.getId(), event.getRemindTarget());
            return;
        }

        // 为每个目标用户构造提醒记录
        for (Long userId : targetUserIds) {
            RemindLogDO log0 = new RemindLogDO();
            log0.setEventId(event.getId());
            log0.setFamilyId(event.getFamilyId());
            log0.setUserId(userId);
            log0.setTriggerYear(currentYear);
            log0.setEventDate(eventDateThisYear);
            log0.setRemindDate(remindDateThisYear);
            log0.setStatus(RemindLogDO.STATUS_PENDING);
            logsToInsert.add(log0);
        }

        log.info("[ProcessEvent] 命中提醒窗口. eventId={}, title={}, eventDate={}, remindDate={}, targets={}",
                event.getId(), event.getTitle(), eventDateThisYear, remindDateThisYear, targetUserIds.size());
    }

    /**
     * 根据事项的日期类型和月/日计算当年的实际阳历日期
     *
     * @return 计算后的阳历日期，无法计算时返回 null
     */
    private LocalDate calcEventDate(EventDO event, int currentYear) {
        int month = event.getMonth();
        int day = event.getDay();

        if (EventDO.DATE_TYPE_LUNAR == event.getDateType()) {
            // 农历：调用工具类转换
            return lunarCalendarUtil.lunarToSolar(currentYear, month, day);
        }

        // 阳历：直接构造，处理非法日期（如 2 月 30 日）
        try {
            return LocalDate.of(currentYear, month, day);
        } catch (DateTimeException e) {
            // 处理无效阳历日期，取该月最后一天（如 2/29 → 非闰年取 2/28）
            try {
                LocalDate firstDay = LocalDate.of(currentYear, month, 1);
                LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
                log.warn("[CalcEventDate] 阳历日期无效，取月末: eventId={}, {}/{}/{} → {}",
                        event.getId(), currentYear, month, day, lastDay);
                return lastDay;
            } catch (DateTimeException e2) {
                log.error("[CalcEventDate] 月份无效，无法计算. eventId={}, month={}", event.getId(), month);
                return null;
            }
        }
    }

    /**
     * 根据事项的提醒对象配置确定目标用户 ID 列表
     */
    private List<Long> resolveTargetUserIds(EventDO event, Map<Long, List<Long>> familyUserMap) {
        if (EventDO.REMIND_TARGET_SPECIFIC.equals(event.getRemindTarget())) {
            // 指定某人：仅提醒 remindUserId
            if (event.getRemindUserId() != null) {
                List<Long> ids = new ArrayList<>();
                ids.add(event.getRemindUserId());
                return ids;
            }
            return Collections.emptyList();
        }
        // ALL：提醒该家庭全体 APPROVED 成员
        return familyUserMap.get(event.getFamilyId());
    }

    /**
     * 关闭所有已过期（event_date < today）且仍处于 PENDING/READ 状态的提醒记录
     * DONE 和 CLOSED_BY_USER 均为终态，不受此步骤影响
     */
    private void closeExpiredLogs() {
        LocalDate today = LocalDate.now();
        log.info("[CloseExpiredLogs] 开始关闭过期提醒, today={}", today);

        List<RemindLogDO> expiredLogs = remindLogMapper.selectExpiredPending(today);
        if (expiredLogs == null || expiredLogs.isEmpty()) {
            log.info("[CloseExpiredLogs] 无过期待关闭记录");
            return;
        }

        List<Long> ids = expiredLogs.stream()
                .map(RemindLogDO::getId)
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();
        int totalClosed = 0;

        // 分批关闭，避免单次 IN 子句过长
        for (int i = 0; i < ids.size(); i += BATCH_CLOSE_SIZE) {
            int end = Math.min(i + BATCH_CLOSE_SIZE, ids.size());
            List<Long> batch = ids.subList(i, end);
            totalClosed += remindLogMapper.batchCloseBySys(batch, now);
        }

        log.info("[CloseExpiredLogs] 关闭完成, 查到过期记录={}, 实际关闭={}", ids.size(), totalClosed);
    }
}
