package com.my.family.paxl.service;

import com.my.family.paxl.domain.vo.RemindHistoryVO;
import com.my.family.paxl.domain.vo.RemindLogVO;

import java.util.List;

/**
 * 提醒业务服务接口
 *
 * @author ai
 * @date 2026/03/18
 */
public interface RemindService {

    /**
     * 统计指定用户在指定家庭的活跃提醒数量（PENDING + READ）
     * 用于家庭主页红点展示
     *
     * @param currentUserId 当前登录用户ID
     * @param familyId      家庭ID
     * @return 活跃提醒数量
     */
    int countActiveReminds(Long currentUserId, Long familyId);

    /**
     * 查询活跃提醒列表（PENDING + READ），同时将 PENDING 批量标记为 READ
     * 用于家庭主页浮层展开时调用
     *
     * @param currentUserId 当前登录用户ID
     * @param familyId      家庭ID
     * @return 活跃提醒 VO 列表，按 event_date ASC 排序
     */
    List<RemindLogVO> listActiveReminds(Long currentUserId, Long familyId);

    /**
     * 用户标记提醒为"已完成"
     * 终态：DONE；同时写入 action_time；校验归属防越权
     *
     * @param currentUserId 当前登录用户ID
     * @param remindLogId   提醒记录ID
     */
    void doneRemind(Long currentUserId, Long remindLogId);

    /**
     * 用户关闭/忽略一条提醒
     * 终态：CLOSED_BY_USER；同时写入 action_time；校验归属防越权
     *
     * @param currentUserId 当前登录用户ID
     * @param remindLogId   提醒记录ID
     */
    void closeRemind(Long currentUserId, Long remindLogId);

    /**
     * 查询某事项当前用户的历年提醒记录（含全部状态，按 trigger_year DESC）
     * 用于重要事项详情页"历年完成情况"展示
     * 校验：currentUserId 必须是该事项所属家庭的 APPROVED 成员
     *
     * @param currentUserId 当前登录用户ID
     * @param eventId       事项ID
     * @return 历年提醒记录 VO 列表
     */
    List<RemindHistoryVO> listRemindHistory(Long currentUserId, Long eventId);
}
