package com.my.family.paxl.service;

import com.my.family.paxl.domain.entity.FamilyDO;
import com.my.family.paxl.domain.entity.FamilyMemberDO;
import com.my.family.paxl.domain.vo.FamilyBriefVO;

import java.util.List;

/**
 * 家庭服务接口
 * 提供创建家庭、加入家庭、审批成员、查询家庭列表等能力
 *
 * @author ai
 * @date 2026/03/13
 */
public interface FamilyService {

    /**
     * 创建家庭
     *
     * @param ownerUserId 户主用户ID
     * @param familyName  家庭名称
     * @param meetDate    相识相遇日期（可为 null，格式 YYYY-MM-DD）
     * @return 创建后的家庭对象
     */
    FamilyDO createFamily(Long ownerUserId, String familyName, String meetDate);

    /**
     * 按家庭编号查询家庭（用于加入前预览）
     *
     * @param familyCode 家庭编号
     * @return 家庭对象，不存在返回 null
     */
    FamilyDO getByFamilyCode(String familyCode);

    /**
     * 申请加入家庭
     *
     * @param userId     当前用户ID
     * @param familyCode 家庭编号
     */
    void applyToJoin(Long userId, String familyCode);

    /**
     * 查询当前户主在指定家庭下的待审批成员列表
     *
     * @param ownerUserId 户主用户ID
     * @param familyId    家庭ID
     * @return 待审批成员关系列表
     */
    List<FamilyMemberDO> listPendingMembers(Long ownerUserId, Long familyId);

    /**
     * 户主审批通过成员加入
     *
     * @param ownerUserId    户主用户ID
     * @param familyId       家庭ID
     * @param familyMemberId 成员关系ID
     */
    void approveMember(Long ownerUserId, Long familyId, Long familyMemberId);

    /**
     * 户主拒绝成员加入
     *
     * @param ownerUserId    户主用户ID
     * @param familyId       家庭ID
     * @param familyMemberId 成员关系ID
     */
    void rejectMember(Long ownerUserId, Long familyId, Long familyMemberId);

    /**
     * 查询当前用户已加入的家庭列表
     *
     * @param userId 用户ID
     * @return 家庭简要信息列表
     */
    List<FamilyBriefVO> listMyFamilies(Long userId);
}

