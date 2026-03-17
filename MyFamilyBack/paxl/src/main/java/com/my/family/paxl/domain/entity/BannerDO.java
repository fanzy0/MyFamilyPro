package com.my.family.paxl.domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 轮播图片数据对象，对应数据库表：t_banner
 * 主要用于存储首页轮播图片的基础信息
 *
 * @author fan
 * @date 2026/01/05
 */
public class BannerDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    private Long id;

    /**
     * 轮播图业务ID，数字递增
     */
    private Long bannerId;

    /**
     * 所属家庭ID，对应 mf_family.id
     */
    private Long familyId;

    /**
     * 图片存储路径（相对路径）
     */
    private String imagePath;

    /**
     * 图片位置，用于排序，数字越小越靠前
     */
    private Integer position;

    /**
     * 图片标题
     */
    private String title;

    /**
     * 图片描述
     */
    private String description;

    /**
     * 点击跳转链接
     */
    private String linkUrl;

    /**
     * 状态：0-禁用，1-启用
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

    public Long getBannerId() {
        return bannerId;
    }

    public void setBannerId(Long bannerId) {
        this.bannerId = bannerId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
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
        return "BannerDO{" +
                "id=" + id +
                ", bannerId=" + bannerId +
                ", familyId=" + familyId +
                ", imagePath='" + imagePath + '\'' +
                ", position=" + position +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", linkUrl='" + linkUrl + '\'' +
                ", status=" + status +
                ", deleted=" + deleted +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}

