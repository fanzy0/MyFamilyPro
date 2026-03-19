package com.my.family.paxl.domain.vo;

import java.io.Serializable;

/**
 * 创建家庭重要事项请求体
 *
 * @author ai
 * @date 2026/03/17
 */
public class CreateEventRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 所属家庭ID（必填）
     */
    private Long familyId;

    /**
     * 事项名称（必填）
     */
    private String title;

    /**
     * 事项类别（必填）
     * 可选值：BIRTHDAY/ANNIVERSARY/HOLIDAY/DOCUMENT_EXPIRY/HEALTH/PAYMENT/OTHER
     */
    private String category;

    /**
     * 日期类型（必填）：0=阳历，1=农历
     */
    private Integer dateType;

    /**
     * 月份（必填，1-12）
     * dateType=0 表示阳历月；dateType=1 表示农历月
     */
    private Integer month;

    /**
     * 日期（必填，1-31）
     * dateType=0 表示阳历日；dateType=1 表示农历日
     */
    private Integer day;

    /**
     * 事项描述/备注（选填）
     */
    private String description;

    /**
     * 是否开启提醒：0-否，1-是（默认 0）
     */
    private Integer remindEnabled;

    /**
     * 提前几天提醒（默认 0，即当天提醒）
     */
    private Integer remindAdvanceDays;

    /**
     * 提醒对象（默认 ALL）：ALL=全部家庭成员，SPECIFIC=指定某人
     */
    private String remindTarget;

    /**
     * 指定提醒用户ID（remindTarget=SPECIFIC 时必填）
     */
    private Long remindUserId;

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
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
}
