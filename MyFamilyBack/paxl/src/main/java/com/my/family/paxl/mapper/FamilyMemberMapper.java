package com.my.family.paxl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.family.paxl.domain.entity.FamilyMemberDO;
import com.my.family.paxl.domain.vo.FamilyBriefVO;
import com.my.family.paxl.domain.vo.FamilyMemberBriefVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 家庭成员关系表数据访问层
 * 所有 SQL 均在 FamilyMemberMapper.xml 中手动编写
 *
 * @author ai
 * @date 2026/03/13
 */
@Mapper
public interface FamilyMemberMapper extends BaseMapper<FamilyMemberDO> {

    /**
     * 新增家庭成员关系（含申请）
     *
     * @param familyMemberDO 家庭成员数据对象
     * @return 影响行数
     */
    int insertMember(FamilyMemberDO familyMemberDO);

    /**
     * 根据家庭ID和用户ID查询成员关系
     *
     * @param familyId 家庭ID
     * @param userId   用户ID
     * @return 成员关系对象，不存在返回 null
     */
    FamilyMemberDO selectByFamilyIdAndUserId(@Param("familyId") Long familyId, @Param("userId") Long userId);

    /**
     * 根据主键ID查询成员关系
     *
     * @param id 成员关系主键ID
     * @return 成员关系对象，不存在返回 null
     */
    FamilyMemberDO selectById(@Param("id") Long id);

    /**
     * 更新成员的加入状态及加入时间
     *
     * @param familyMemberDO 包含 id、joinStatus、joinTime 的对象
     * @return 影响行数
     */
    int updateJoinStatus(FamilyMemberDO familyMemberDO);

    /**
     * 查询指定家庭下待审批的成员列表
     *
     * @param familyId 家庭ID
     * @return 成员关系列表
     */
    List<FamilyMemberDO> selectPendingMembers(@Param("familyId") Long familyId);

    /**
     * 查询用户已加入的家庭列表（用于 familyList）
     *
     * @param userId 用户ID
     * @return 家庭简要信息列表
     */
    List<FamilyBriefVO> selectFamiliesByUserId(@Param("userId") Long userId);

    /**
     * 批量查询指定家庭列表中所有 APPROVED 成员（定时扫描任务使用）
     * 返回包含 familyId 和 userId 的成员列表，一次查询覆盖多个家庭
     *
     * @param familyIds 家庭ID列表（不能为空）
     * @return 成员关系列表（只填充 familyId / userId 字段）
     */
    List<FamilyMemberDO> selectApprovedMembersByFamilyIds(@Param("familyIds") List<Long> familyIds);

    /**
     * 查询指定家庭下所有已通过（APPROVED）的成员简要信息（含昵称）
     *
     * @param familyId 家庭ID
     * @return 成员简要列表
     */
    List<FamilyMemberBriefVO> selectApprovedMemberBriefs(@Param("familyId") Long familyId);
}

