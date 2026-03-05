package com.my.family.paxl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.family.paxl.domain.entity.BannerDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 轮播图片表数据访问层
 * 负责轮播图片基础信息的增删改查
 *
 * @author fan
 * @date 2026/01/05
 */
@Mapper
public interface BannerMapper extends BaseMapper<BannerDO> {

    /**
     * 新增轮播图片
     *
     * @param bannerDO 轮播图片数据对象
     * @return 影响行数
     */
    int insert(BannerDO bannerDO);

    /**
     * 根据主键ID更新轮播图片信息
     *
     * @param bannerDO 轮播图片数据对象，必须包含id
     * @return 影响行数
     */
    int updateById(BannerDO bannerDO);

    /**
     * 根据轮播图ID（业务编号）查询轮播图片信息
     *
     * @param bannerId 轮播图ID，业务编号
     * @return 轮播图片数据对象，不存在返回null
     */
    BannerDO selectByBannerId(@Param("bannerId") Long bannerId);

    /**
     * 查询当前未删除轮播图中最大的轮播图ID（banner_id）
     *
     * @return 最大的轮播图ID，如果没有数据则返回null
     */
    Long selectMaxBannerId();

    /**
     * 查询所有启用的轮播图片，按位置排序
     *
     * @return 轮播图片列表
     */
    List<BannerDO> selectEnabledBanners();

    /**
     * 根据主键ID逻辑删除轮播图片
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}

