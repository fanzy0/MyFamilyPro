package com.my.family.paxl.service.impl;

import com.my.family.paxl.domain.entity.EventDO;
import com.my.family.paxl.domain.entity.FamilyMemberDO;
import com.my.family.paxl.domain.vo.CreateEventRequest;
import com.my.family.paxl.domain.vo.EventBriefVO;
import com.my.family.paxl.domain.vo.EventDetailVO;
import com.my.family.paxl.mapper.EventMapper;
import com.my.family.paxl.mapper.FamilyMemberMapper;
import com.my.family.paxl.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 家庭重要事项服务实现
 *
 * @author ai
 * @date 2026/03/17
 */
@Service
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventMapper eventMapper;
    private final FamilyMemberMapper familyMemberMapper;

    public EventServiceImpl(EventMapper eventMapper, FamilyMemberMapper familyMemberMapper) {
        this.eventMapper = eventMapper;
        this.familyMemberMapper = familyMemberMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDetailVO createEvent(Long currentUserId, CreateEventRequest request) {
        log.info("[CreateEvent] 开始创建事项, userId={}, familyId={}, title={}", currentUserId, request.getFamilyId(), request.getTitle());

        // 1. 参数基础校验
        if (request.getFamilyId() == null) {
            throw new IllegalArgumentException("家庭ID不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("事项名称不能为空");
        }
        if (!StringUtils.hasText(request.getCategory())) {
            throw new IllegalArgumentException("事项类别不能为空");
        }
        if (request.getDateType() == null) {
            throw new IllegalArgumentException("日期类型不能为空");
        }
        if (request.getMonth() == null || request.getDay() == null) {
            throw new IllegalArgumentException("月和日不能为空");
        }
        if (request.getMonth() < 1 || request.getMonth() > 12) {
            throw new IllegalArgumentException("月份必须在 1-12 之间");
        }
        if (request.getDay() < 1 || request.getDay() > 31) {
            throw new IllegalArgumentException("日期必须在 1-31 之间");
        }

        // 2. 校验当前用户是该家庭的正式成员
        checkApprovedMember(currentUserId, request.getFamilyId());

        // 3. 构建 DO
        EventDO event = new EventDO();
        event.setFamilyId(request.getFamilyId());
        event.setCreatorUserId(currentUserId);
        event.setTitle(request.getTitle().trim());
        event.setCategory(request.getCategory());
        event.setDateType(request.getDateType());
        event.setMonth(request.getMonth());
        event.setDay(request.getDay());
        event.setDescription(request.getDescription());
        event.setRemindEnabled(request.getRemindEnabled() != null ? request.getRemindEnabled() : 0);
        event.setRemindAdvanceDays(request.getRemindAdvanceDays() != null ? request.getRemindAdvanceDays() : 0);
        event.setRemindTarget(StringUtils.hasText(request.getRemindTarget()) ? request.getRemindTarget() : EventDO.REMIND_TARGET_ALL);
        event.setRemindUserId(request.getRemindUserId());

        // 4. 持久化
        eventMapper.insertEvent(event);
        log.info("[CreateEvent] 创建成功, eventId={}, userId={}, familyId={}", event.getId(), currentUserId, request.getFamilyId());

        // 5. 返回完整详情
        EventDetailVO detail = eventMapper.selectDetailById(event.getId());
        if (detail != null) {
            detail.setIsOwner(true);
        }
        return detail;
    }

    @Override
    public List<EventBriefVO> listEvents(Long currentUserId, Long familyId) {
        log.info("[ListEvents] 查询事项列表, userId={}, familyId={}", currentUserId, familyId);

        if (familyId == null) {
            throw new IllegalArgumentException("家庭ID不能为空");
        }

        // 校验成员身份
        checkApprovedMember(currentUserId, familyId);

        List<EventBriefVO> list = eventMapper.selectByFamilyId(familyId);

        // 标记是否为当前用户创建的事项
        for (EventBriefVO vo : list) {
            vo.setIsOwner(currentUserId.equals(vo.getCreatorUserId()));
        }

        log.info("[ListEvents] 查询完成, userId={}, familyId={}, count={}", currentUserId, familyId, list.size());
        return list;
    }

    @Override
    public EventDetailVO getEventDetail(Long currentUserId, Long eventId) {
        log.info("[GetEventDetail] 查询事项详情, userId={}, eventId={}", currentUserId, eventId);

        if (eventId == null) {
            throw new IllegalArgumentException("事项ID不能为空");
        }

        EventDetailVO detail = eventMapper.selectDetailById(eventId);
        if (detail == null) {
            throw new IllegalStateException("事项不存在或已删除");
        }

        // 校验当前用户是该事项所属家庭的成员
        checkApprovedMember(currentUserId, detail.getFamilyId());

        detail.setIsOwner(currentUserId.equals(detail.getCreatorUserId()));
        log.info("[GetEventDetail] 查询成功, userId={}, eventId={}", currentUserId, eventId);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEvent(Long currentUserId, Long eventId) {
        log.info("[DeleteEvent] 删除事项, userId={}, eventId={}", currentUserId, eventId);

        if (eventId == null) {
            throw new IllegalArgumentException("事项ID不能为空");
        }

        EventDetailVO detail = eventMapper.selectDetailById(eventId);
        if (detail == null) {
            throw new IllegalStateException("事项不存在或已删除");
        }

        // 仅创建人可删除
        if (!currentUserId.equals(detail.getCreatorUserId())) {
            log.warn("[DeleteEvent] 无权删除, userId={}, eventId={}, creatorUserId={}", currentUserId, eventId, detail.getCreatorUserId());
            throw new SecurityException("只有创建人才能删除该事项");
        }

        eventMapper.logicDeleteById(eventId);
        log.info("[DeleteEvent] 删除成功, userId={}, eventId={}", currentUserId, eventId);
    }

    /**
     * 校验用户是否为家庭的正式成员（join_status=APPROVED）
     *
     * @param userId   用户ID
     * @param familyId 家庭ID
     */
    private void checkApprovedMember(Long userId, Long familyId) {
        FamilyMemberDO member = familyMemberMapper.selectByFamilyIdAndUserId(familyId, userId);
        if (member == null || !FamilyMemberDO.STATUS_APPROVED.equals(member.getJoinStatus())) {
            log.warn("[CheckApprovedMember] 成员身份校验失败, userId={}, familyId={}", userId, familyId);
            throw new SecurityException("您不是该家庭的成员，无权操作");
        }
    }
}
