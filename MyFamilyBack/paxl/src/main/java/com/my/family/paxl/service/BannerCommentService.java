package com.my.family.paxl.service;

import com.my.family.paxl.domain.entity.BannerCommentDO;

import java.util.List;

/**
 * 轮播图评论服务接口
 * 提供评论新增、查询能力
 *
 * @author fan
 * @date 2026/01/05
 */
public interface BannerCommentService {

    /**
     * 保存评论
     *
     * @param commentDO 评论数据对象，必须包含bannerId和content
     * @return 保存后的评论对象
     */
    BannerCommentDO saveComment(BannerCommentDO commentDO);

    /**
     * 根据轮播图ID查询评论列表
     *
     * @param bannerId 轮播图ID
     * @return 评论列表，按创建时间倒序
     */
    List<BannerCommentDO> listCommentsByBannerId(Long bannerId);
}

