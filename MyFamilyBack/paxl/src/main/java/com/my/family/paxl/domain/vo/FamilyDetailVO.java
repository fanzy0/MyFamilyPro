package com.my.family.paxl.domain.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 家庭详情视图对象：用于展示家庭名称、编号、成员列表等
 */
public class FamilyDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long familyId;
    private String familyName;
    private String familyCode;
    private Integer memberCount;
    private String currentUserRole;
    private List<FamilyMemberBriefVO> members;

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

    public String getCurrentUserRole() {
        return currentUserRole;
    }

    public void setCurrentUserRole(String currentUserRole) {
        this.currentUserRole = currentUserRole;
    }

    public List<FamilyMemberBriefVO> getMembers() {
        return members;
    }

    public void setMembers(List<FamilyMemberBriefVO> members) {
        this.members = members;
    }
}

