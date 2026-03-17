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
     * 会校验当前用户是否为 bannerDO.familyId 对应家庭的 APPROVED 成员
     *
     * @param bannerDO 轮播图片数据对象，必须包含 imagePath 和 familyId
     * @param userId   当前操作用户ID，来自 UserContext
     * @return 保存后的轮播图片对象
     */
    BannerDO saveBanner(BannerDO bannerDO, Long userId);

    /**
     * 删除轮播图片
     * 逻辑删除，会校验当前用户是否为该 banner 所属家庭的 APPROVED 成员
     *
     * @param bannerId 轮播图ID，业务编号
     * @param userId   当前操作用户ID，来自 UserContext
     */
    void deleteBanner(Long bannerId, Long userId);

    /**
     * 查询指定家庭下所有启用的轮播图片
     * 会校验当前用户是否为该家庭的 APPROVED 成员
     *
     * @param familyId 家庭ID
     * @param userId   当前操作用户ID，来自 UserContext
     * @return 轮播图片列表，按位置排序
     */
    List<BannerDO> listEnabledBanners(Long familyId, Long userId);

    /**
     * 根据轮播图ID查询轮播图片信息
     *
     * @param bannerId 轮播图ID，业务编号
     * @return 轮播图片对象，不存在返回null
     */
    BannerDO getBannerById(Long bannerId);
}

