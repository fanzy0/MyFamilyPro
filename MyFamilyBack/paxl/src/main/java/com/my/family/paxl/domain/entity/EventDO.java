package com.my.family.paxl.domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭重要事项数据对象，对应数据库表：mf_event
 * 包含事项基础信息及提醒配置字段（提醒调度逻辑在 Plan 七实现）
 *
 * @author ai
 * @date 2026/03/17
 */
public class EventDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提醒对象：全部家庭成员
     */
    public static final String REMIND_TARGET_ALL = "ALL";

    /**
     * 提醒对象：指定某人
     */
    public static final String REMIND_TARGET_SPECIFIC = "SPECIFIC";

    /**
     * 日期类型：阳历
     */
    public static final int DATE_TYPE_SOLAR = 0;

    /**
     * 日期类型：农历
     */
    public static final int DATE_TYPE_LUNAR = 1;

    /**
     * 主键ID，自增
     */
    private Long id;

    /**
     * 所属家庭ID，关联 mf_family.id
     */
    private Long familyId;

    /**
     * 创建人用户ID，关联 mf_user.id
     */
    private Long creatorUserId;

    /**
     * 事项名称
     */
    private String title;

    /**
     * 事项类别
     * 内置枚举：BIRTHDAY/ANNIVERSARY/HOLIDAY/DOCUMENT_EXPIRY/HEALTH/PAYMENT/OTHER
     */
    private String category;

    /**
     * 日期类型：0-阳历，1-农历
     */
    private Integer dateType;

    /**
     * 月份（1-12）
     * dateType=0 表示阳历月；dateType=1 表示农历月
     */
    private Integer month;

    /**
     * 日期（1-31）
     * dateType=0 表示阳历日；dateType=1 表示农历日
     */
    private Integer day;

    /**
     * 事项描述/备注
     */
    private String description;

    /**
     * 是否开启提醒：0-否，1-是
     */
    private Integer remindEnabled;

    /**
     * 提前几天提醒（0=当天提醒）（Plan 七启用）
     */
    private Integer remindAdvanceDays;

    /**
     * 提醒对象：ALL-全部家庭成员，SPECIFIC-指定某人（Plan 七启用）
     */
    private String remindTarget;

    /**
     * 指定提醒用户ID（remindTarget=SPECIFIC 时有效）（Plan 七启用）
     */
    private Long remindUserId;

    /**
     * 状态：0-正常，1-禁用
     */
    private Integer status;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;

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

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public Long getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(Long creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getDateType() {
        return dateType;
    }

    public void setDateType(Integer dateType) {
        this.dateType = dateType;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRemindEnabled() {
        return remindEnabled;
    }

    public void setRemindEnabled(Integer remindEnabled) {
        this.remindEnabled = remindEnabled;
    }

    public Integer getRemindAdvanceDays() {
        return remindAdvanceDays;
    }

    public void setRemindAdvanceDays(Integer remindAdvanceDays) {
        this.remindAdvanceDays = remindAdvanceDays;
    }

    public String getRemindTarget() {
        return remindTarget;
    }

    public void setRemindTarget(String remindTarget) {
        this.remindTarget = remindTarget;
    }

    public Long getRemindUserId() {
        return remindUserId;
    }

    public void setRemindUserId(Long remindUserId) {
        this.remindUserId = remindUserId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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
        return "EventDO{" +
                "id=" + id +
                ", familyId=" + familyId +
                ", creatorUserId=" + creatorUserId +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", dateType=" + dateType +
                ", month=" + month +
                ", day=" + day +
                ", remindEnabled=" + remindEnabled +
                '}';
    }
}
