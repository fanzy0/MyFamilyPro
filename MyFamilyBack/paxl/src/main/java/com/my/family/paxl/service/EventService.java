package com.my.family.paxl.service;

import com.my.family.paxl.domain.vo.CreateEventRequest;
import com.my.family.paxl.domain.vo.EventBriefVO;
import com.my.family.paxl.domain.vo.EventDetailVO;

import java.util.List;

/**
 * 家庭重要事项服务接口
 *
 * @author ai
 * @date 2026/03/17
 */
public interface EventService {

    /**
     * 创建重要事项
     * 要求：当前用户必须是该家庭 join_status=APPROVED 的成员
     *
     * @param currentUserId 当前登录用户ID
     * @param request       创建请求体
     * @return 创建成功后的事项详情
     */
    EventDetailVO createEvent(Long currentUserId, CreateEventRequest request);

    /**
     * 查询家庭事项列表
     * 要求：当前用户必须是该家庭 APPROVED 成员
     *
     * @param currentUserId 当前登录用户ID
     * @param familyId      家庭ID
     * @return 事项列表（含 isOwner 标记）
     */
    List<EventBriefVO> listEvents(Long currentUserId, Long familyId);

    /**
     * 查询事项详情
     * 要求：当前用户必须是事项所属家庭的 APPROVED 成员
     *
     * @param currentUserId 当前登录用户ID
     * @param eventId       事项ID
     * @return 事项详情（含 isOwner 标记）
     */
    EventDetailVO getEventDetail(Long currentUserId, Long eventId);

    /**
     * 删除事项（逻辑删除，仅创建人可操作）
     * 要求：creator_user_id == currentUserId，否则拒绝
     *
     * @param currentUserId 当前登录用户ID
     * @param eventId       事项ID
     */
    void deleteEvent(Long currentUserId, Long eventId);
}
