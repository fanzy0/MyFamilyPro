package com.my.family.paxl.service.impl;

import com.my.family.paxl.domain.entity.BannerCommentDO;
import com.my.family.paxl.mapper.BannerCommentMapper;
import com.my.family.paxl.service.BannerCommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @desc 轮播图评论服务实现类，负责评论的增删改查和业务逻辑处理
 * @author fan
 * @date 2026/01/05
 */
@Service
@Slf4j
public class BannerCommentServiceImpl implements BannerCommentService {

    @Resource
    private BannerCommentMapper bannerCommentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BannerCommentDO saveComment(BannerCommentDO commentDO) {
        log.info("[SaveComment] 开始保存评论, commentDO={}", commentDO);

        // 步骤1 - 参数校验
        if (commentDO == null) {
            log.warn("[SaveComment] 评论信息为空");
            throw new IllegalArgumentException("评论信息不能为空");
        }

        if (commentDO.getBannerId() == null || commentDO.getBannerId() <= 0) {
            log.warn("[SaveComment] 轮播图ID不合法, bannerId={}", commentDO.getBannerId());
            throw new IllegalArgumentException("轮播图ID不能为空");
        }

        if (!StringUtils.hasText(commentDO.getContent())) {
            log.warn("[SaveComment] 评论内容为空");
            throw new IllegalArgumentException("评论内容不能为空");
        }

        // 步骤2 - 生成业务ID
        Long maxCommentId = bannerCommentMapper.selectMaxCommentId();
        Long commentId = (maxCommentId == null) ? 1L : maxCommentId + 1;
        commentDO.setCommentId(commentId);

        // 步骤3 - 设置默认值
        if (commentDO.getStatus() == null) {
            commentDO.setStatus(1); // 默认显示
        }
        if (commentDO.getDeleted() == null) {
            commentDO.setDeleted(0);
        }
        LocalDateTime now = LocalDateTime.now();
        if (commentDO.getCreateTime() == null) {
            commentDO.setCreateTime(now);
        }
        if (commentDO.getUpdateTime() == null) {
            commentDO.setUpdateTime(now);
        }

        // 步骤4 - 保存到数据库
        int result = bannerCommentMapper.insert(commentDO);
        if (result <= 0) {
            log.error("[SaveComment] 保存评论失败, commentDO={}", commentDO);
            throw new RuntimeException("保存评论失败");
        }

        log.info("[SaveComment] 保存评论成功, commentId={}, id={}", commentId, commentDO.getId());
        return commentDO;
    }

    @Override
    public List<BannerCommentDO> listCommentsByBannerId(Long bannerId) {
        log.info("[ListCommentsByBannerId] 开始查询评论列表, bannerId={}", bannerId);

        if (bannerId == null || bannerId <= 0) {
            log.warn("[ListCommentsByBannerId] 轮播图ID不合法, bannerId={}", bannerId);
            throw new IllegalArgumentException("轮播图ID不合法");
        }

        List<BannerCommentDO> comments = bannerCommentMapper.selectByBannerId(bannerId);
        log.info("[ListCommentsByBannerId] 查询成功, bannerId={}, count={}", bannerId, comments.size());
        return comments;
    }
}

