package com.my.family.paxl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.family.paxl.domain.entity.BannerCommentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 轮播图评论表数据访问层
 * 负责轮播图评论信息的增删改查
 *
 * @author fan
 * @date 2026/01/05
 */
@Mapper
public interface BannerCommentMapper extends BaseMapper<BannerCommentDO> {

    /**
     * 新增评论
     *
     * @param commentDO 评论数据对象
     * @return 影响行数
     */
    int insert(BannerCommentDO commentDO);

    /**
     * 根据轮播图ID查询评论列表
     *
     * @param bannerId 轮播图ID
     * @return 评论列表
     */
    List<BannerCommentDO> selectByBannerId(@Param("bannerId") Long bannerId);

    /**
     * 查询当前未删除评论中最大的评论ID（comment_id）
     *
     * @return 最大的评论ID，如果没有数据则返回null
     */
    Long selectMaxCommentId();

    /**
     * 根据主键ID逻辑删除评论
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}

