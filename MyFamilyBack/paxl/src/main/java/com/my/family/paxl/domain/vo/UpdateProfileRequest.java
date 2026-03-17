package com.my.family.paxl.domain.vo;

import java.io.Serializable;

/**
 * 更新用户基础资料请求对象
 * nickname 和 avatarUrl 均为可选，仅更新非空字段
 *
 * @author ai
 * @date 2026/03/13
 */
public class UpdateProfileRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户昵称（可选，用户通过 type="nickname" 输入框主动设置）
     */
    private String nickname;

    /**
     * 用户头像地址（可选，用户通过 open-type="chooseAvatar" 选择后上传）
     */
    private String avatarUrl;

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
}
