package com.my.family.paxl.domain.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭数据对象，对应数据库表：mf_family
 * 存储家庭基础信息（名称、编号、户主、成员数量等）
 *
 * @author ai
 * @date 2026/03/13
 */
public class FamilyDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 家庭状态：正常
     */
    public static final int STATUS_NORMAL = 0;

    /**
     * 家庭状态：禁用
     */
    public static final int STATUS_DISABLED = 1;

    /**
     * 家庭状态：已解散
     */
    public static final int STATUS_DISMISSED = 2;

    /**
     * 主键ID，自增
     */
    private Long id;

    /**
     * 家庭名称
     */
    private String familyName;

    /**
     * 家庭编号（短码），用于成员加入
     */
    private String familyCode;

    /**
     * 户主用户ID，对应 mf_user.id
     */
    private Long ownerUserId;

    /**
     * 成员数量，包含户主在内
     */
    private Integer memberCount;

    /**
     * 相识相遇日期，可为空；用于首页展示"一起走过的天数"
     */
    private LocalDate meetDate;

    /**
     * 状态：0-正常，1-禁用，2-已解散
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getFamilyCode() {
        return familyCode;
    }

    public void setFamilyCode(String familyCode) {
        this.familyCode = familyCode;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public LocalDate getMeetDate() {
        return meetDate;
    }

    public void setMeetDate(LocalDate meetDate) {
        this.meetDate = meetDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
}

