package com.my.family.paxl.domain.vo;

import java.io.Serializable;

/**
 * 用户基础信息视图对象，用于接口响应，不包含敏感字段
 *
 * @author ai
 * @date 2026/03/13
 */
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户主键 ID
     */
    private Long id;

    /**
     * 用户昵称，可为空（用户未主动设置时为空）
     */
    private String nickname;

    /**
     * 用户头像地址，可为空（用户未主动设置时为空）
     */
    private String avatarUrl;

    /**
     * 用户状态：0-正常，1-禁用
     */
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
