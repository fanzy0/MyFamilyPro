package com.my.family.paxl.domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭成员关系数据对象，对应数据库表：mf_family_member
 * 承载成员与家庭的关系，以及加入申请状态
 *
 * @author ai
 * @date 2026/03/13
 */
public class FamilyMemberDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色：户主
     */
    public static final String ROLE_OWNER = "OWNER";

    /**
     * 角色：普通成员
     */
    public static final String ROLE_MEMBER = "MEMBER";

    /**
     * 加入状态：待审批
     */
    public static final String STATUS_PENDING = "PENDING";

    /**
     * 加入状态：已通过
     */
    public static final String STATUS_APPROVED = "APPROVED";

    /**
     * 加入状态：已拒绝
     */
    public static final String STATUS_REJECTED = "REJECTED";

    /**
     * 加入状态：已退出
     */
    public static final String STATUS_QUIT = "QUIT";

    /**
     * 主键ID，自增
     */
    private Long id;

    /**
     * 家庭ID，对应 mf_family.id
     */
    private Long familyId;

    /**
     * 用户ID，对应 mf_user.id
     */
    private Long userId;

    /**
     * 角色：OWNER-户主，MEMBER-成员
     */
    private String role;

    /**
     * 加入状态：PENDING/APPROVED/REJECTED/QUIT
     */
    private String joinStatus;

    /**
     * 加入时间（审批通过时间）
     */
    private LocalDateTime joinTime;

    /**
     * 创建时间（申请时间）
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getJoinStatus() {
        return joinStatus;
    }

    public void setJoinStatus(String joinStatus) {
        this.joinStatus = joinStatus;
    }

    public LocalDateTime getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
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

