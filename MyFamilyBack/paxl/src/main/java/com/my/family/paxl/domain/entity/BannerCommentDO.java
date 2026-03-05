package com.my.family.paxl.domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 轮播图评论数据对象，对应数据库表：t_banner_comment
 * 主要用于存储轮播图片的评论信息
 *
 * @author fan
 * @date 2026/01/05
 */
public class BannerCommentDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    private Long id;

    /**
     * 评论业务ID，数字递增
     */
    private Long commentId;

    /**
     * 关联的轮播图ID
     */
    private Long bannerId;

    /**
     * 评论用户名称
     */
    private String userName;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 状态：0-隐藏，1-显示
     */
    private Integer status;

    /**
     * 是否删除标记：0-未删除，1-已删除
     */
    private Integer deleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getBannerId() {
        return bannerId;
    }

    public void setBannerId(Long bannerId) {
        this.bannerId = bannerId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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
        return "BannerCommentDO{" +
                "id=" + id +
                ", commentId=" + commentId +
                ", bannerId=" + bannerId +
                ", userName='" + userName + '\'' +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", deleted=" + deleted +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}

