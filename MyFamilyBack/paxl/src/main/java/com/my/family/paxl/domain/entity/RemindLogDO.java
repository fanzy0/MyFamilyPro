package com.my.family.paxl.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提醒记录数据对象，对应数据库表：mf_remind_log
 * 每条记录对应「一个用户 × 一个事项 × 一年」的提醒实例
 *
 * @author ai
 * @date 2026/03/18
 */
@TableName("mf_remind_log")
public class RemindLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态：已生成，用户未查看/未操作
     */
    public static final String STATUS_PENDING = "PENDING";

    /**
     * 状态：用户已查看（展开浮层时自动标记）
     */
    public static final String STATUS_READ = "READ";

    /**
     * 状态：用户主动标记"已完成"（计入历年完成记录）
     */
    public static final String STATUS_DONE = "DONE";

    /**
     * 状态：用户主动关闭/忽略本次提醒（不计入完成）
     */
    public static final String STATUS_CLOSED_BY_USER = "CLOSED_BY_USER";

    /**
     * 状态：系统在 event_date 过后自动关闭
     */
    public static final String STATUS_CLOSED_BY_SYSTEM = "CLOSED_BY_SYSTEM";

    /**
     * 主键ID，自增
     */
    private Long id;

    /**
     * 关联事项ID，对应 mf_event.id
     */
    private Long eventId;

    /**
     * 家庭ID（冗余字段），便于按家庭查询，避免多表 JOIN
     */
    private Long familyId;

    /**
     * 被提醒的用户ID，对应 mf_user.id
     */
    private Long userId;

    /**
     * 本条记录对应的年份（如 2026）；与 event_id/user_id 联合去重
     */
    private Integer triggerYear;

    /**
     * 当年事件实际发生日期（已转换为阳历）；过期判断依据
     */
    private LocalDate eventDate;

    /**
     * 当年应开始提醒日期（event_date - remind_advance_days）
     */
    private LocalDate remindDate;

    /**
     * 状态：PENDING / READ / DONE / CLOSED_BY_USER / CLOSED_BY_SYSTEM
     */
    private String status;

    /**
     * 用户或系统产生终态动作的时间（DONE/关闭时写入，非终态为 null）
     */
    private LocalDateTime actionTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // ===== getter / setter =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getTriggerYear() {
        return triggerYear;
    }

    public void setTriggerYear(Integer triggerYear) {
        this.triggerYear = triggerYear;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDate getRemindDate() {
        return remindDate;
    }

    public void setRemindDate(LocalDate remindDate) {
        this.remindDate = remindDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "RemindLogDO{" +
                "id=" + id +
                ", eventId=" + eventId +
                ", familyId=" + familyId +
                ", userId=" + userId +
                ", triggerYear=" + triggerYear +
                ", eventDate=" + eventDate +
                ", status='" + status + '\'' +
                '}';
    }
}
