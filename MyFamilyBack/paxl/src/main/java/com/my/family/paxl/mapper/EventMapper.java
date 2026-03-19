package com.my.family.paxl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.family.paxl.domain.entity.EventDO;
import com.my.family.paxl.domain.vo.EventBriefVO;
import com.my.family.paxl.domain.vo.EventDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 家庭重要事项 Mapper 接口，对应数据库表：mf_event
 * 所有 SQL 在 EventMapper.xml 中手动编写
 *
 * @author ai
 * @date 2026/03/17
 */
@Mapper
public interface EventMapper extends BaseMapper<EventDO> {

    /**
     * 新增重要事项
     *
     * @param eventDO 事项数据对象
     * @return 影响行数
     */
    int insertEvent(EventDO eventDO);

    /**
     * 查询指定家庭的事项列表（含创建人昵称），按 month/day 升序排列
     *
     * @param familyId 家庭ID
     * @return 事项列表（列表项 VO）
     */
    List<EventBriefVO> selectByFamilyId(@Param("familyId") Long familyId);

    /**
     * 根据主键查询事项详情（含创建人昵称和头像）
     *
     * @param id 事项主键ID
     * @return 事项详情 VO，不存在时返回 null
     */
    EventDetailVO selectDetailById(@Param("id") Long id);

    /**
     * 逻辑删除事项（将 deleted 置为 1）
     *
     * @param id 事项主键ID
     * @return 影响行数
     */
    int logicDeleteById(@Param("id") Long id);

    /**
     * 查询所有已开启提醒且有效的事项（定时扫描任务使用）
     * 条件：remind_enabled=1 AND status=0 AND deleted=0
     *
     * @return 有效事项列表
     */
    List<EventDO> selectAllActiveWithRemind();
}
