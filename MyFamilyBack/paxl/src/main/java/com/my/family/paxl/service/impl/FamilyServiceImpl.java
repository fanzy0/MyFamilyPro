package com.my.family.paxl.service.impl;

import com.my.family.paxl.domain.entity.FamilyDO;
import com.my.family.paxl.domain.entity.FamilyMemberDO;
import com.my.family.paxl.domain.vo.FamilyBriefVO;
import com.my.family.paxl.domain.vo.FamilyDetailVO;
import com.my.family.paxl.domain.vo.FamilyMemberBriefVO;
import com.my.family.paxl.mapper.FamilyMapper;
import com.my.family.paxl.mapper.FamilyMemberMapper;
import com.my.family.paxl.service.FamilyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * 家庭服务实现
 *
 * @author ai
 * @date 2026/03/13
 */
@Service
@Slf4j
public class FamilyServiceImpl implements FamilyService {

    private final FamilyMapper familyMapper;
    private final FamilyMemberMapper familyMemberMapper;

    public FamilyServiceImpl(FamilyMapper familyMapper, FamilyMemberMapper familyMemberMapper) {
        this.familyMapper = familyMapper;
        this.familyMemberMapper = familyMemberMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyDO createFamily(Long ownerUserId, String familyName, String meetDateStr) {
        log.info("[CreateFamily] 开始创建家庭, ownerUserId={}, familyName={}, meetDate={}", ownerUserId, familyName, meetDateStr);

        if (ownerUserId == null || ownerUserId <= 0) {
            log.warn("[CreateFamily] 户主用户ID不合法, ownerUserId={}", ownerUserId);
            throw new IllegalArgumentException("户主用户ID不合法");
        }
        if (!StringUtils.hasText(familyName)) {
            log.warn("[CreateFamily] 家庭名称为空");
            throw new IllegalArgumentException("家庭名称不能为空");
        }

        // 解析可选的相识日期，格式为 YYYY-MM-DD；解析失败时忽略（不阻断创建流程）
        LocalDate meetDate = null;
        if (StringUtils.hasText(meetDateStr)) {
            try {
                meetDate = LocalDate.parse(meetDateStr.trim());
            } catch (DateTimeParseException e) {
                log.warn("[CreateFamily] meetDate 格式不合法，已忽略, meetDateStr={}", meetDateStr);
            }
        }

        String familyCode = generateFamilyCode();
        LocalDateTime now = LocalDateTime.now();

        FamilyDO family = new FamilyDO();
        family.setFamilyName(familyName.trim());
        family.setFamilyCode(familyCode);
        family.setOwnerUserId(ownerUserId);
        family.setMemberCount(1);
        family.setMeetDate(meetDate);
        family.setStatus(FamilyDO.STATUS_NORMAL);
        family.setCreateTime(now);
        family.setUpdateTime(now);

        familyMapper.insertFamily(family);

        FamilyMemberDO owner = new FamilyMemberDO();
        owner.setFamilyId(family.getId());
        owner.setUserId(ownerUserId);
        owner.setRole(FamilyMemberDO.ROLE_OWNER);
        owner.setJoinStatus(FamilyMemberDO.STATUS_APPROVED);
        owner.setJoinTime(now);
        owner.setCreateTime(now);
        owner.setUpdateTime(now);
        familyMemberMapper.insertMember(owner);

        log.info("[CreateFamily] 创建家庭成功, familyId={}, familyCode={}", family.getId(), family.getFamilyCode());
        return family;
    }

    @Override
    public FamilyDO getByFamilyCode(String familyCode) {
        log.info("[GetByFamilyCode] 查询家庭, familyCode={}", familyCode);
        if (!StringUtils.hasText(familyCode)) {
            throw new IllegalArgumentException("家庭编号不能为空");
        }
        return familyMapper.selectByFamilyCode(familyCode.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyToJoin(Long userId, String familyCode) {
        log.info("[ApplyToJoin] 申请加入家庭, userId={}, familyCode={}", userId, familyCode);

        if (userId == null || userId <= 0) {
            log.warn("[ApplyToJoin] 用户ID不合法, userId={}", userId);
            throw new IllegalArgumentException("用户ID不合法");
        }
        if (!StringUtils.hasText(familyCode)) {
            log.warn("[ApplyToJoin] 家庭编号为空");
            throw new IllegalArgumentException("家庭编号不能为空");
        }

        FamilyDO family = familyMapper.selectByFamilyCode(familyCode.trim());
        if (family == null || family.getStatus() == null || family.getStatus() != FamilyDO.STATUS_NORMAL) {
            log.warn("[ApplyToJoin] 家庭不存在或不可用, familyCode={}", familyCode);
            throw new IllegalArgumentException("家庭不存在或不可用");
        }

        FamilyMemberDO existing = familyMemberMapper.selectByFamilyIdAndUserId(family.getId(), userId);
        if (existing != null) {
            if (FamilyMemberDO.STATUS_APPROVED.equals(existing.getJoinStatus())) {
                log.warn("[ApplyToJoin] 用户已在家庭中, familyId={}, userId={}", family.getId(), userId);
                throw new IllegalArgumentException("您已在该家庭中");
            }
            if (FamilyMemberDO.STATUS_PENDING.equals(existing.getJoinStatus())) {
                log.warn("[ApplyToJoin] 用户已申请加入, familyId={}, userId={}", family.getId(), userId);
                throw new IllegalArgumentException("您已申请加入该家庭，请等待户主审批");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            FamilyMemberDO member = new FamilyMemberDO();
            member.setFamilyId(family.getId());
            member.setUserId(userId);
            member.setRole(FamilyMemberDO.ROLE_MEMBER);
            member.setJoinStatus(FamilyMemberDO.STATUS_PENDING);
            member.setCreateTime(now);
            member.setUpdateTime(now);
            familyMemberMapper.insertMember(member);
        } else {
            existing.setJoinStatus(FamilyMemberDO.STATUS_PENDING);
            existing.setJoinTime(null);
            existing.setUpdateTime(now);
            familyMemberMapper.updateJoinStatus(existing);
        }

        log.info("[ApplyToJoin] 提交加入申请成功, familyId={}, userId={}", family.getId(), userId);
    }

    @Override
    public List<FamilyMemberDO> listPendingMembers(Long ownerUserId, Long familyId) {
        log.info("[ListPendingMembers] 查询待审批成员, ownerUserId={}, familyId={}", ownerUserId, familyId);

        assertOwner(ownerUserId, familyId);
        return familyMemberMapper.selectPendingMembers(familyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveMember(Long ownerUserId, Long familyId, Long familyMemberId) {
        log.info("[ApproveMember] 审批通过成员, ownerUserId={}, familyId={}, familyMemberId={}",
                ownerUserId, familyId, familyMemberId);

        assertOwner(ownerUserId, familyId);

        FamilyMemberDO member = familyMemberMapper.selectById(familyMemberId);
        if (member == null || !familyId.equals(member.getFamilyId())) {
            log.warn("[ApproveMember] 成员记录不存在或不属于该家庭, familyMemberId={}, familyId={}", familyMemberId, familyId);
            throw new IllegalArgumentException("成员记录不存在");
        }
        if (!FamilyMemberDO.STATUS_PENDING.equals(member.getJoinStatus())) {
            log.warn("[ApproveMember] 成员状态不是待审批, familyMemberId={}, status={}", familyMemberId, member.getJoinStatus());
            throw new IllegalArgumentException("成员状态已变化，请刷新后重试");
        }

        LocalDateTime now = LocalDateTime.now();
        member.setJoinStatus(FamilyMemberDO.STATUS_APPROVED);
        member.setJoinTime(now);
        member.setUpdateTime(now);
        familyMemberMapper.updateJoinStatus(member);

        familyMapper.incrementMemberCount(familyId);

        log.info("[ApproveMember] 审批通过成功, familyId={}, familyMemberId={}", familyId, familyMemberId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectMember(Long ownerUserId, Long familyId, Long familyMemberId) {
        log.info("[RejectMember] 审批拒绝成员, ownerUserId={}, familyId={}, familyMemberId={}",
                ownerUserId, familyId, familyMemberId);

        assertOwner(ownerUserId, familyId);

        FamilyMemberDO member = familyMemberMapper.selectById(familyMemberId);
        if (member == null || !familyId.equals(member.getFamilyId())) {
            log.warn("[RejectMember] 成员记录不存在或不属于该家庭, familyMemberId={}, familyId={}", familyMemberId, familyId);
            throw new IllegalArgumentException("成员记录不存在");
        }
        if (!FamilyMemberDO.STATUS_PENDING.equals(member.getJoinStatus())) {
            log.warn("[RejectMember] 成员状态不是待审批, familyMemberId={}, status={}", familyMemberId, member.getJoinStatus());
            throw new IllegalArgumentException("成员状态已变化，请刷新后重试");
        }

        LocalDateTime now = LocalDateTime.now();
        member.setJoinStatus(FamilyMemberDO.STATUS_REJECTED);
        member.setUpdateTime(now);
        familyMemberMapper.updateJoinStatus(member);

        log.info("[RejectMember] 审批拒绝成功, familyId={}, familyMemberId={}", familyId, familyMemberId);
    }

    @Override
    public List<FamilyBriefVO> listMyFamilies(Long userId) {
        log.info("[ListMyFamilies] 查询用户家庭列表, userId={}", userId);
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID不合法");
        }
        return familyMemberMapper.selectFamiliesByUserId(userId);
    }

    @Override
    public FamilyDetailVO getFamilyDetail(Long userId, Long familyId) {
        log.info("[GetFamilyDetail] 查询家庭详情, userId={}, familyId={}", userId, familyId);
        if (userId == null || userId <= 0 || familyId == null || familyId <= 0) {
            throw new IllegalArgumentException("参数不合法");
        }

        FamilyMemberDO relation = familyMemberMapper.selectByFamilyIdAndUserId(familyId, userId);
        if (relation == null || !FamilyMemberDO.STATUS_APPROVED.equals(relation.getJoinStatus())) {
            throw new IllegalArgumentException("您未加入该家庭，无法查看详情");
        }

        FamilyDO family = familyMapper.selectById(familyId);
        if (family == null || family.getStatus() == null || family.getStatus() != FamilyDO.STATUS_NORMAL) {
            throw new IllegalArgumentException("家庭不存在或不可用");
        }

        List<FamilyMemberBriefVO> members = familyMemberMapper.selectApprovedMemberBriefs(familyId);

        FamilyDetailVO vo = new FamilyDetailVO();
        vo.setFamilyId(family.getId());
        vo.setFamilyName(family.getFamilyName());
        vo.setFamilyCode(family.getFamilyCode());
        vo.setMemberCount(family.getMemberCount());
        vo.setCurrentUserRole(relation.getRole());
        vo.setMembers(members);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFamilyInfo(Long ownerUserId, Long familyId, String familyName, String meetDateStr) {
        log.info("[UpdateFamilyInfo] 更新家庭信息, ownerUserId={}, familyId={}, familyName={}, meetDate={}",
                ownerUserId, familyId, familyName, meetDateStr);

        if (ownerUserId == null || ownerUserId <= 0 || familyId == null || familyId <= 0) {
            throw new IllegalArgumentException("参数不合法");
        }
        if (!StringUtils.hasText(familyName) || familyName.trim().length() < 2 || familyName.trim().length() > 64) {
            throw new IllegalArgumentException("家庭名称长度需为 2~64");
        }

        // 仅户主可操作
        assertOwner(ownerUserId, familyId);

        // meetDate 允许为空；非空时校验格式
        String meetDate = null;
        if (StringUtils.hasText(meetDateStr)) {
            try {
                LocalDate.parse(meetDateStr.trim());
                meetDate = meetDateStr.trim();
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("meetDate 格式不合法");
            }
        }

        FamilyDO family = familyMapper.selectById(familyId);
        if (family == null || family.getStatus() == null || family.getStatus() != FamilyDO.STATUS_NORMAL) {
            throw new IllegalArgumentException("家庭不存在或不可用");
        }

        int rows = familyMapper.updateFamilyInfo(familyId, familyName.trim(), meetDate);
        if (rows <= 0) {
            throw new IllegalArgumentException("更新失败，请稍后重试");
        }
    }

    /**
     * 校验当前用户是否为指定家庭的户主
     *
     * @param ownerUserId 户主用户ID
     * @param familyId    家庭ID
     */
    private void assertOwner(Long ownerUserId, Long familyId) {
        if (ownerUserId == null || ownerUserId <= 0 || familyId == null || familyId <= 0) {
            throw new IllegalArgumentException("参数不合法");
        }
        FamilyMemberDO relation = familyMemberMapper.selectByFamilyIdAndUserId(familyId, ownerUserId);
        if (relation == null
                || !FamilyMemberDO.ROLE_OWNER.equals(relation.getRole())
                || !FamilyMemberDO.STATUS_APPROVED.equals(relation.getJoinStatus())) {
            log.warn("[AssertOwner] 当前用户不是该家庭户主, ownerUserId={}, familyId={}", ownerUserId, familyId);
            throw new IllegalArgumentException("当前用户不是该家庭户主，无法执行此操作");
        }
    }

    /**
     * 生成家庭编号（短码）
     *
     * @return 家庭编号
     */
    private String generateFamilyCode() {
        // 使用 UUID 取前 8 位，转大写，碰撞概率极低
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        // 为安全起见，最多重试几次避免极端碰撞
        int retry = 0;
        while (familyMapper.selectByFamilyCode(code) != null && retry < 3) {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            retry++;
        }
        return code;
    }
}

