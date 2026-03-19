package com.my.family.paxl.domain.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭重要事项列表项视图对象（不含提醒配置详情）
 * 用于事项列表接口返回
 *
 * @author ai
 * @date 2026/03/17
 */
public class EventBriefVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事项主键ID
     */
    private Long id;

    /**
     * 所属家庭ID
     */
    private Long familyId;

    /**
     * 事项名称
     */
    private String title;

    /**
     * 事项类别
     */
    private String category;

    /**
     * 日期类型：0-阳历，1-农历
     */
    private Integer dateType;

    /**
     * 月份（1-12）
     */
    private Integer month;

    /**
     * 日期（1-31）
     */
    private Integer day;

    /**
     * 是否开启提醒：0-否，1-是
     */
    private Integer remindEnabled;

    /**
     * 创建人用户ID
     */
    private Long creatorUserId;

    /**
     * 创建人昵称（JOIN mf_user 获取）
     */
    private String creatorNickname;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 是否为当前登录用户创建（true=可执行删除操作）
     */
    private Boolean isOwner;

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

    public Integer getRemindEnabled() {
        return remindEnabled;
    }

    public void setRemindEnabled(Integer remindEnabled) {
        this.remindEnabled = remindEnabled;
    }

    public Long getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(Long creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public String getCreatorNickname() {
        return creatorNickname;
    }

    public void setCreatorNickname(String creatorNickname) {
        this.creatorNickname = creatorNickname;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Boolean getIsOwner() {
        return isOwner;
    }

    public void setIsOwner(Boolean isOwner) {
        this.isOwner = isOwner;
    }
}
