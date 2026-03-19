package com.my.family.paxl.service.impl;

import com.my.family.paxl.domain.entity.FamilyMemberDO;
import com.my.family.paxl.domain.entity.RemindLogDO;
import com.my.family.paxl.domain.vo.RemindHistoryVO;
import com.my.family.paxl.domain.vo.RemindLogVO;
import com.my.family.paxl.mapper.FamilyMemberMapper;
import com.my.family.paxl.mapper.RemindLogMapper;
import com.my.family.paxl.service.RemindService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒业务服务实现
 *
 * @author ai
 * @date 2026/03/18
 */
@Service
@Slf4j
public class RemindServiceImpl implements RemindService {

    private final RemindLogMapper remindLogMapper;
    private final FamilyMemberMapper familyMemberMapper;

    public RemindServiceImpl(RemindLogMapper remindLogMapper,
                             FamilyMemberMapper familyMemberMapper) {
        this.remindLogMapper = remindLogMapper;
        this.familyMemberMapper = familyMemberMapper;
    }

    @Override
    public int countActiveReminds(Long currentUserId, Long familyId) {
        log.info("[CountActiveReminds] 统计活跃提醒数, userId={}, familyId={}", currentUserId, familyId);

        checkApprovedMember(currentUserId, familyId);

        int count = remindLogMapper.countActiveByUserAndFamily(currentUserId, familyId);
        log.info("[CountActiveReminds] 统计完成, userId={}, familyId={}, count={}", currentUserId, familyId, count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RemindLogVO> listActiveReminds(Long currentUserId, Long familyId) {
        log.info("[ListActiveReminds] 查询活跃提醒列表, userId={}, familyId={}", currentUserId, familyId);

        checkApprovedMember(currentUserId, familyId);

        // 先查列表，再批量将 PENDING → READ（标记用户已查看）
        List<RemindLogVO> list = remindLogMapper.selectActiveByUserAndFamily(currentUserId, familyId);
        int updated = remindLogMapper.batchMarkReadByUserAndFamily(currentUserId, familyId);
        log.info("[ListActiveReminds] 查询完成, userId={}, familyId={}, count={}, markedRead={}",
                currentUserId, familyId, list.size(), updated);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doneRemind(Long currentUserId, Long remindLogId) {
        log.info("[DoneRemind] 标记已完成, userId={}, remindLogId={}", currentUserId, remindLogId);

        RemindLogDO log0 = checkExistsAndOwner(currentUserId, remindLogId);
        checkNotTerminal(log0, remindLogId);

        int affected = remindLogMapper.markDone(remindLogId, currentUserId, LocalDateTime.now());
        if (affected == 0) {
            log.warn("[DoneRemind] 操作影响行数为0（可能已是终态）, remindLogId={}", remindLogId);
        } else {
            log.info("[DoneRemind] 标记成功, userId={}, remindLogId={}", currentUserId, remindLogId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeRemind(Long currentUserId, Long remindLogId) {
        log.info("[CloseRemind] 用户关闭提醒, userId={}, remindLogId={}", currentUserId, remindLogId);

        RemindLogDO log0 = checkExistsAndOwner(currentUserId, remindLogId);
        checkNotTerminal(log0, remindLogId);

        int affected = remindLogMapper.closeByUser(remindLogId, currentUserId, LocalDateTime.now());
        if (affected == 0) {
            log.warn("[CloseRemind] 操作影响行数为0（可能已是终态）, remindLogId={}", remindLogId);
        } else {
            log.info("[CloseRemind] 关闭成功, userId={}, remindLogId={}", currentUserId, remindLogId);
        }
    }

    @Override
    public List<RemindHistoryVO> listRemindHistory(Long currentUserId, Long eventId) {
        log.info("[ListRemindHistory] 查询历年提醒记录, userId={}, eventId={}", currentUserId, eventId);

        // 直接按 userId + eventId 过滤，只返回当前用户的记录，天然防越权
        List<RemindHistoryVO> list = remindLogMapper.selectHistoryByEvent(eventId, currentUserId);
        log.info("[ListRemindHistory] 查询完成, userId={}, eventId={}, count={}", currentUserId, eventId, list.size());
        return list;
    }

    /**
     * 校验提醒记录存在且归属于当前用户，返回记录对象
     */
    private RemindLogDO checkExistsAndOwner(Long currentUserId, Long remindLogId) {
        RemindLogDO log0 = remindLogMapper.selectById(remindLogId);
        if (log0 == null) {
            log.warn("[CheckExistsAndOwner] 提醒记录不存在, remindLogId={}", remindLogId);
            throw new IllegalStateException("提醒记录不存在: " + remindLogId);
        }
        if (!currentUserId.equals(log0.getUserId())) {
            log.warn("[CheckExistsAndOwner] 无权操作他人提醒, userId={}, remindLogId={}, ownerId={}",
                    currentUserId, remindLogId, log0.getUserId());
            throw new SecurityException("无权操作他人的提醒记录");
        }
        return log0;
    }

    /**
     * 校验提醒记录不处于终态，终态不可再流转
     */
    private void checkNotTerminal(RemindLogDO log0, Long remindLogId) {
        String status = log0.getStatus();
        boolean isTerminal = RemindLogDO.STATUS_DONE.equals(status)
                || RemindLogDO.STATUS_CLOSED_BY_USER.equals(status)
                || RemindLogDO.STATUS_CLOSED_BY_SYSTEM.equals(status);
        if (isTerminal) {
            log.warn("[CheckNotTerminal] 提醒记录已是终态，无法再操作, remindLogId={}, status={}", remindLogId, status);
            throw new IllegalStateException("该提醒记录已处于终态（" + status + "），无法再操作");
        }
    }

    /**
     * 校验用户是否为家庭的正式成员（join_status=APPROVED）
     */
    private void checkApprovedMember(Long userId, Long familyId) {
        FamilyMemberDO member = familyMemberMapper.selectByFamilyIdAndUserId(familyId, userId);
        if (member == null || !FamilyMemberDO.STATUS_APPROVED.equals(member.getJoinStatus())) {
            log.warn("[CheckApprovedMember] 成员身份校验失败, userId={}, familyId={}", userId, familyId);
            throw new SecurityException("您不是该家庭的成员，无权操作");
        }
    }
}
