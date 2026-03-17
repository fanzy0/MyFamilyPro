package com.my.family.paxl.domain.vo;

import java.io.Serializable;

/**
 * 家庭简要信息视图对象，用于 my-families 接口返回给前端
 * 包含家庭基础展示信息和当前用户的角色
 *
 * @author ai
 * @date 2026/03/13
 */
public class FamilyBriefVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 家庭主键 ID
     */
    private Long familyId;

    /**
     * 家庭名称
     */
    private String familyName;

    /**
     * 家庭编号（短码），成员可分享给他人用于加入
     */
    private String familyCode;

    /**
     * 成员数量，包含户主在内
     */
    private Integer memberCount;

    /**
     * 相识相遇日期（格式 YYYY-MM-DD），可为 null；用于首页展示"一起走过的天数"
     */
    private String meetDate;

    /**
     * 当前用户在此家庭中的角色：OWNER-户主，MEMBER-成员
     */
    private String role;

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
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

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public String getMeetDate() {
        return meetDate;
    }

    public void setMeetDate(String meetDate) {
        this.meetDate = meetDate;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
