package com.my.family.paxl.service.impl;

import com.my.family.paxl.domain.entity.BannerDO;
import com.my.family.paxl.mapper.BannerMapper;
import com.my.family.paxl.service.BannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @desc 轮播图片服务实现类，负责轮播图片的增删改查和业务逻辑处理
 * @author fan
 * @date 2026/01/05
 */
@Service
@Slf4j
public class BannerServiceImpl implements BannerService {

    @Resource
    private BannerMapper bannerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BannerDO saveBanner(BannerDO bannerDO) {
        log.info("[SaveBanner] 开始保存轮播图片, bannerDO={}", bannerDO);

        // 步骤1 - 参数校验
        if (bannerDO == null) {
            log.warn("[SaveBanner] 轮播图片信息为空");
            throw new IllegalArgumentException("轮播图片信息不能为空");
        }

        if (!StringUtils.hasText(bannerDO.getImagePath())) {
            log.warn("[SaveBanner] 图片路径为空");
            throw new IllegalArgumentException("图片路径不能为空");
        }

        // 步骤2 - 生成业务ID
        Long maxBannerId = bannerMapper.selectMaxBannerId();
        Long bannerId = (maxBannerId == null) ? 1L : maxBannerId + 1;
        bannerDO.setBannerId(bannerId);

        // 步骤3 - 设置默认值
        if (bannerDO.getPosition() == null) {
            bannerDO.setPosition(0);
        }
        if (bannerDO.getStatus() == null) {
            bannerDO.setStatus(1); // 默认启用
        }
        if (bannerDO.getDeleted() == null) {
            bannerDO.setDeleted(0);
        }
        LocalDateTime now = LocalDateTime.now();
        if (bannerDO.getCreateTime() == null) {
            bannerDO.setCreateTime(now);
        }
        if (bannerDO.getUpdateTime() == null) {
            bannerDO.setUpdateTime(now);
        }

        // 步骤4 - 保存到数据库
        int result = bannerMapper.insert(bannerDO);
        if (result <= 0) {
            log.error("[SaveBanner] 保存轮播图片失败, bannerDO={}", bannerDO);
            throw new RuntimeException("保存轮播图片失败");
        }

        log.info("[SaveBanner] 保存轮播图片成功, bannerId={}, id={}", bannerId, bannerDO.getId());
        return bannerDO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBanner(Long bannerId) {
        log.info("[DeleteBanner] 开始删除轮播图片, bannerId={}", bannerId);

        // 步骤1 - 参数校验
        if (bannerId == null || bannerId <= 0) {
            log.warn("[DeleteBanner] 轮播图ID不合法, bannerId={}", bannerId);
            throw new IllegalArgumentException("轮播图ID不合法");
        }

        // 步骤2 - 查询轮播图片
        BannerDO bannerDO = bannerMapper.selectByBannerId(bannerId);
        if (bannerDO == null) {
            log.warn("[DeleteBanner] 轮播图片不存在, bannerId={}", bannerId);
            throw new IllegalArgumentException("轮播图片不存在");
        }

        // 步骤3 - 逻辑删除
        int result = bannerMapper.deleteById(bannerDO.getId());
        if (result <= 0) {
            log.error("[DeleteBanner] 删除轮播图片失败, bannerId={}", bannerId);
            throw new RuntimeException("删除轮播图片失败");
        }

        log.info("[DeleteBanner] 删除轮播图片成功, bannerId={}", bannerId);
    }

    @Override
    public List<BannerDO> listEnabledBanners() {
        log.info("[ListEnabledBanners] 开始查询启用的轮播图片列表");

        List<BannerDO> banners = bannerMapper.selectEnabledBanners();
        log.info("[ListEnabledBanners] 查询成功, count={}", banners.size());
        return banners;
    }

    @Override
    public BannerDO getBannerById(Long bannerId) {
        log.info("[GetBannerById] 开始查询轮播图片, bannerId={}", bannerId);

        if (bannerId == null || bannerId <= 0) {
            log.warn("[GetBannerById] 轮播图ID不合法, bannerId={}", bannerId);
            throw new IllegalArgumentException("轮播图ID不合法");
        }

        BannerDO bannerDO = bannerMapper.selectByBannerId(bannerId);
        log.info("[GetBannerById] 查询完成, bannerId={}, exists={}", bannerId, bannerDO != null);
        return bannerDO;
    }
}

