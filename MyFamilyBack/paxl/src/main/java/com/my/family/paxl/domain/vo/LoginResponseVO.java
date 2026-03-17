package com.my.family.paxl.domain.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 登录接口响应视图对象
 * 包含用户基础信息和已加入的家庭列表
 *
 * @author ai
 * @date 2026/03/13
 */
public class LoginResponseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户基础信息
     */
    private UserInfoVO user;

    /**
     * 用户已加入的家庭列表
     * 首版始终为空数组，与家庭模块打通后填充
     */
    private List<FamilyBriefVO> familyList;

    public UserInfoVO getUser() {
        return user;
    }

    public void setUser(UserInfoVO user) {
        this.user = user;
    }

    public List<FamilyBriefVO> getFamilyList() {
        return familyList;
    }

    public void setFamilyList(List<FamilyBriefVO> familyList) {
        this.familyList = familyList;
    }
}
