package com.my.family.paxl.service.impl;

import com.my.family.paxl.domain.entity.BannerDO;
import com.my.family.paxl.domain.entity.FamilyMemberDO;
import com.my.family.paxl.mapper.BannerMapper;
import com.my.family.paxl.mapper.FamilyMemberMapper;
import com.my.family.paxl.service.BannerService;
import lombok.extern.slf4j.Slf4j;
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

    private static final String JOIN_STATUS_APPROVED = "APPROVED";

    @Resource
    private BannerMapper bannerMapper;

    @Resource
    private FamilyMemberMapper familyMemberMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BannerDO saveBanner(BannerDO bannerDO, Long userId) {
        log.info("[SaveBanner] 开始保存轮播图片, userId={}, bannerDO={}", userId, bannerDO);

        // 步骤1 - 参数校验
        if (bannerDO == null) {
            throw new IllegalArgumentException("轮播图片信息不能为空");
        }
        if (!StringUtils.hasText(bannerDO.getImagePath())) {
            throw new IllegalArgumentException("图片路径不能为空");
        }
        if (bannerDO.getFamilyId() == null || bannerDO.getFamilyId() <= 0) {
            throw new IllegalArgumentException("家庭ID不能为空");
        }

        // 步骤2 - 校验用户是否为该家庭的成员
        checkFamilyMembership(bannerDO.getFamilyId(), userId, "SaveBanner");

        // 步骤3 - 生成业务ID
        Long maxBannerId = bannerMapper.selectMaxBannerId();
        bannerDO.setBannerId((maxBannerId == null) ? 1L : maxBannerId + 1);

        // 步骤4 - 设置默认值
        if (bannerDO.getPosition() == null) {
            bannerDO.setPosition(0);
        }
        if (bannerDO.getStatus() == null) {
            bannerDO.setStatus(1);
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

        // 步骤5 - 保存到数据库
        int result = bannerMapper.insert(bannerDO);
        if (result <= 0) {
            log.error("[SaveBanner] 保存轮播图片失败, bannerDO={}", bannerDO);
            throw new RuntimeException("保存轮播图片失败");
        }

        log.info("[SaveBanner] 保存轮播图片成功, bannerId={}, id={}", bannerDO.getBannerId(), bannerDO.getId());
        return bannerDO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBanner(Long bannerId, Long userId) {
        log.info("[DeleteBanner] 开始删除轮播图片, bannerId={}, userId={}", bannerId, userId);

        // 步骤1 - 参数校验
        if (bannerId == null || bannerId <= 0) {
            throw new IllegalArgumentException("轮播图ID不合法");
        }

        // 步骤2 - 查询轮播图片（同时获取 familyId 用于归属校验）
        BannerDO bannerDO = bannerMapper.selectByBannerId(bannerId);
        if (bannerDO == null) {
            throw new IllegalArgumentException("轮播图片不存在");
        }

        // 步骤3 - 校验用户是否为该 banner 所属家庭的成员
        checkFamilyMembership(bannerDO.getFamilyId(), userId, "DeleteBanner");

        // 步骤4 - 逻辑删除
        int result = bannerMapper.deleteById(bannerDO.getId());
        if (result <= 0) {
            log.error("[DeleteBanner] 删除轮播图片失败, bannerId={}", bannerId);
            throw new RuntimeException("删除轮播图片失败");
        }

        log.info("[DeleteBanner] 删除轮播图片成功, bannerId={}", bannerId);
    }

    @Override
    public List<BannerDO> listEnabledBanners(Long familyId, Long userId) {
        log.info("[ListEnabledBanners] 开始查询启用的轮播图片列表, familyId={}, userId={}", familyId, userId);

        if (familyId == null || familyId <= 0) {
            throw new IllegalArgumentException("家庭ID不能为空");
        }

        // 校验用户是否为该家庭的成员
        checkFamilyMembership(familyId, userId, "ListEnabledBanners");

        List<BannerDO> banners = bannerMapper.selectEnabledBanners(familyId);
        log.info("[ListEnabledBanners] 查询成功, familyId={}, count={}", familyId, banners.size());
        return banners;
    }

    /**
     * 校验用户是否为指定家庭的 APPROVED 成员，不满足时抛出异常
     */
    private void checkFamilyMembership(Long familyId, Long userId, String logTag) {
        FamilyMemberDO member = familyMemberMapper.selectByFamilyIdAndUserId(familyId, userId);
        if (member == null || !JOIN_STATUS_APPROVED.equals(member.getJoinStatus())) {
            log.warn("[{}] 用户无权限操作该家庭的轮播图, familyId={}, userId={}", logTag, familyId, userId);
            throw new IllegalArgumentException("无权限操作该家庭的轮播图");
        }
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

