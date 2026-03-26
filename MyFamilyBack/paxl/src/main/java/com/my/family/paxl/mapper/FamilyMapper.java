package com.my.family.paxl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.family.paxl.domain.entity.FamilyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 家庭表数据访问层
 * 所有 SQL 均在 FamilyMapper.xml 中手动编写
 *
 * @author ai
 * @date 2026/03/13
 */
@Mapper
public interface FamilyMapper extends BaseMapper<FamilyDO> {

    /**
     * 新增家庭
     *
     * @param familyDO 家庭数据对象
     * @return 影响行数
     */
    int insertFamily(FamilyDO familyDO);

    /**
     * 根据家庭编号查询家庭
     *
     * @param familyCode 家庭编号
     * @return 家庭对象，不存在返回 null
     */
    FamilyDO selectByFamilyCode(@Param("familyCode") String familyCode);

    /**
     * 根据主键 ID 查询家庭
     *
     * @param id 家庭主键 ID
     * @return 家庭对象，不存在返回 null
     */
    FamilyDO selectById(@Param("id") Long id);

    /**
     * 成员数量 +1（仅在审批通过时调用）
     *
     * @param familyId 家庭主键 ID
     * @return 影响行数
     */
    int incrementMemberCount(@Param("familyId") Long familyId);

    /**
     * 更新家庭基础信息（仅名称与相识日期）
     *
     * @param familyId   家庭ID
     * @param familyName 家庭名称
     * @param meetDate   相识相遇日期（可为空，格式 YYYY-MM-DD）
     * @return 影响行数
     */
    int updateFamilyInfo(@Param("familyId") Long familyId,
                         @Param("familyName") String familyName,
                         @Param("meetDate") String meetDate);
}

