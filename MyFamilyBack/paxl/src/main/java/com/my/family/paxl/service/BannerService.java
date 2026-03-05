package com.my.family.paxl.service;

import com.my.family.paxl.domain.entity.BannerDO;

import java.util.List;

/**
 * 轮播图片服务接口
 * 提供轮播图片新增、删除、查询能力
 *
 * @author fan
 * @date 2026/01/05
 */
public interface BannerService {

    /**
     * 保存轮播图片
     * 用户上传图片后，调用此接口保存图片信息到数据库
     *
     * @param bannerDO 轮播图片数据对象，必须包含imagePath
     * @return 保存后的轮播图片对象
     */
    BannerDO saveBanner(BannerDO bannerDO);

    /**
     * 删除轮播图片
     * 逻辑删除，同时删除关联的评论
     *
     * @param bannerId 轮播图ID，业务编号
     */
    void deleteBanner(Long bannerId);

    /**
     * 查询所有启用的轮播图片
     * 返回图片元素信息，不包含图片内容
     *
     * @return 轮播图片列表，按位置排序
     */
    List<BannerDO> listEnabledBanners();

    /**
     * 根据轮播图ID查询轮播图片信息
     *
     * @param bannerId 轮播图ID，业务编号
     * @return 轮播图片对象，不存在返回null
     */
    BannerDO getBannerById(Long bannerId);
}

