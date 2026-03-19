package com.my.family.paxl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.family.paxl.domain.entity.RemindLogDO;
import com.my.family.paxl.domain.vo.RemindHistoryVO;
import com.my.family.paxl.domain.vo.RemindLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒记录 Mapper 接口，对应数据库表：mf_remind_log
 * 所有 SQL 在 RemindLogMapper.xml 中手动编写
 *
 * @author ai
 * @date 2026/03/18
 */
@Mapper
public interface RemindLogMapper extends BaseMapper<RemindLogDO> {

    /**
     * 批量 INSERT IGNORE：唯一索引冲突时静默忽略，保证幂等性
     * 唯一索引：(event_id, user_id, trigger_year)
     *
     * @param logs 提醒记录列表（不能为空）
     * @return 实际插入行数
     */
    int batchInsertIgnore(@Param("logs") List<RemindLogDO> logs);

    /**
     * 查询所有已过期且状态仍为 PENDING/READ 的记录（定时任务关闭用）
     * 条件：event_date < beforeDate AND status IN ('PENDING','READ')
     *
     * @param beforeDate 截止日期（通常传 today）
     * @return 需要被系统关闭的记录列表
     */
    List<RemindLogDO> selectExpiredPending(@Param("beforeDate") LocalDate beforeDate);

    /**
     * 批量将指定记录更新为 CLOSED_BY_SYSTEM，并写入 action_time
     *
     * @param ids        要关闭的记录ID列表（不能为空）
     * @param actionTime 关闭时间
     * @return 影响行数
     */
    int batchCloseBySys(@Param("ids") List<Long> ids, @Param("actionTime") LocalDateTime actionTime);

    /**
     * 查询指定用户在指定家庭的活跃提醒列表（PENDING + READ），JOIN mf_event 补充展示字段
     * 按 event_date ASC 排序
     *
     * @param userId   被提醒的用户ID
     * @param familyId 家庭ID
     * @return 活跃提醒 VO 列表
     */
    List<RemindLogVO> selectActiveByUserAndFamily(@Param("userId") Long userId,
                                                  @Param("familyId") Long familyId);

    /**
     * 统计指定用户在指定家庭的活跃提醒数量（PENDING + READ），用于首页红点展示
     *
     * @param userId   被提醒的用户ID
     * @param familyId 家庭ID
     * @return 活跃提醒数量
     */
    int countActiveByUserAndFamily(@Param("userId") Long userId,
                                   @Param("familyId") Long familyId);

    /**
     * 将指定用户在指定家庭的所有 PENDING 记录批量标记为 READ（用户展开浮层时调用）
     *
     * @param userId   用户ID
     * @param familyId 家庭ID
     * @return 影响行数
     */
    int batchMarkReadByUserAndFamily(@Param("userId") Long userId,
                                     @Param("familyId") Long familyId);

    /**
     * 将单条 PENDING/READ 记录标记为 DONE，并写入 action_time（防越权：校验 user_id）
     *
     * @param id         提醒记录ID
     * @param userId     当前用户ID（防越权）
     * @param actionTime 完成时间
     * @return 影响行数（0 表示记录不存在/越权/已是终态）
     */
    int markDone(@Param("id") Long id,
                 @Param("userId") Long userId,
                 @Param("actionTime") LocalDateTime actionTime);

    /**
     * 将单条 PENDING/READ 记录标记为 CLOSED_BY_USER，并写入 action_time（防越权：校验 user_id）
     *
     * @param id         提醒记录ID
     * @param userId     当前用户ID（防越权）
     * @param actionTime 关闭时间
     * @return 影响行数（0 表示记录不存在/越权/已是终态）
     */
    int closeByUser(@Param("id") Long id,
                    @Param("userId") Long userId,
                    @Param("actionTime") LocalDateTime actionTime);

    /**
     * 查询指定事项指定用户的历年提醒记录（含全部状态），按 trigger_year DESC 排序
     * 用于重要事项详情页"历年完成情况"展示
     *
     * @param eventId 事项ID
     * @param userId  当前用户ID
     * @return 历年提醒记录 VO 列表
     */
    List<RemindHistoryVO> selectHistoryByEvent(@Param("eventId") Long eventId,
                                               @Param("userId") Long userId);

    /**
     * 根据主键查询提醒记录（用于存在性和归属校验）
     *
     * @param id 主键ID
     * @return 提醒记录，不存在返回 null
     */
    RemindLogDO selectById(@Param("id") Long id);
}
