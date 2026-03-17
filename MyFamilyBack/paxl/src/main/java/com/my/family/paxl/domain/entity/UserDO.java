package com.my.family.paxl.domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户数据对象，对应数据库表：mf_user
 * 通过微信云托管注入的 X-WX-OPENID 唯一标识用户，无需密码登录
 *
 * @author ai
 * @date 2026/03/13
 */
public class UserDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户状态：正常
     */
    public static final int STATUS_NORMAL = 0;

    /**
     * 用户状态：禁用
     */
    public static final int STATUS_DISABLED = 1;

    /**
     * 主键ID，自增
     */
    private Long id;

    /**
     * 微信小程序 openid，全局唯一用户标识
     */
    private String openid;

    /**
     * 微信开放平台 unionid，预留字段，首版不处理
     */
    private String unionid;

    /**
     * 用户昵称，由用户主动在个人资料页设置，初始为空
     */
    private String nickname;

    /**
     * 用户头像地址，由用户主动设置，初始为空
     */
    private String avatarUrl;

    /**
     * 手机号，预留字段，首版不处理绑定逻辑
     */
    private String mobile;

    /**
     * 用户状态：0-正常，1-禁用
     */
    private Integer status;

    /**
     * 最近一次登录时间
     */
    private LocalDateTime lastLoginTime;

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

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
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

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
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
        return "UserDO{" +
                "id=" + id +
                ", openid='" + maskOpenid(openid) + '\'' +
                ", nickname='" + nickname + '\'' +
                ", status=" + status +
                ", lastLoginTime=" + lastLoginTime +
                ", createTime=" + createTime +
                '}';
    }

    /**
     * openid 脱敏，仅保留前6位
     *
     * @param openid 原始 openid
     * @return 脱敏后的 openid
     */
    private String maskOpenid(String openid) {
        if (openid == null || openid.length() <= 6) {
            return "***";
        }
        return openid.substring(0, 6) + "***";
    }
}
